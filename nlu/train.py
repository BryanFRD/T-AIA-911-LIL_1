import os
import sys
import json
import tempfile
from pathlib import Path
from transformers import (
    AutoTokenizer,
    AutoModelForTokenClassification,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    DataCollatorForTokenClassification,
    DataCollatorWithPadding,
    pipeline
)
from datasets import Dataset, load_from_disk
import torch
from multiprocessing import freeze_support
import numpy as np
from sklearn.metrics import precision_recall_fscore_support, accuracy_score

# Import NER model with CRF
sys.path.insert(0, str(Path(__file__).parent.parent))
from nlu.layers.ner_crf import CamembertForTokenClassificationWithCRF
from common.mlflow_utils import setup_mlflow, start_run, log_training_config, log_pytorch_model, end_run
from common.training_utils import detect_device, print_system_info, load_config, print_dataset_info, ensure_dir, get_machine_name

# Import MLflow after transformers to avoid conflicts
import mlflow

def main():
    # === MLflow Configuration ===
    experiment_name = os.environ.get("MLFLOW_EXPERIMENT_NAME", "nlu")
    setup_mlflow(experiment_name, enable_system_metrics=True)

    # === Device detection ===
    print_system_info()
    device = detect_device()

    # === Load configuration ===
    CONFIG_FILE = "nlu/config.json"

    # Load config.json (required) with optional config.local.json overrides
    config = load_config(CONFIG_FILE)

    # Get config sections
    train_cfg = config["training"]
    opt_cfg = config.get("optimization", {})

    # === Apply PyTorch optimizations ===
    if device == "cuda":
        # Enable TF32 for faster matmul on Ampere GPUs
        if opt_cfg.get("use_tf32", True):
            torch.backends.cuda.matmul.allow_tf32 = True
            torch.backends.cudnn.allow_tf32 = True
            print("[OPTIMIZATION] TF32 enabled for faster training")

        # Enable cudnn benchmarking for consistent input sizes
        if opt_cfg.get("cudnn_benchmark", True):
            torch.backends.cudnn.benchmark = True
            print("[OPTIMIZATION] cuDNN benchmarking enabled")

    dataset_path = "nlu/data/train_generated.json"
    if os.path.exists(dataset_path):
        with open(dataset_path) as f:
            raw_data = json.load(f)
        full_dataset = Dataset.from_list([{
            "text": " ".join(item["tokens"]),
            "intent": item["intent"],
            "labels_bio": item["labels"]
        } for item in raw_data])
        print(f"Dataset charg: {len(full_dataset)} exemples")

        # Split train/validation (90/10)
        split = full_dataset.train_test_split(test_size=0.1, seed=42)
        dataset = split["train"]
        val_dataset = split["test"]
        print_dataset_info(len(full_dataset), len(dataset), len(val_dataset))
    else:
        print(f"ERREUR: {dataset_path} n'existe pas!")
        return

    # Start MLflow run with custom source name
    # Detect if running in GitLab CI or locally
    if os.environ.get("CI") == "true":
        # GitLab CI: use pipeline ID
        pipeline_id = os.environ.get("CI_PIPELINE_ID", "unknown")
        source_name = f"CI #{pipeline_id}"
    else:
        # Local: use machine hostname
        machine_name = get_machine_name()
        source_name = f"Local ({machine_name})"

    with start_run("nlu-training", source_name=source_name):
        # Log parameters
        log_training_config(config, device)
        mlflow.log_param("use_crf", config["training"].get("use_crf", False))
        mlflow.log_param("num_entity_labels", len(config["entity_labels"]))
        mlflow.log_param("num_intent_labels", len(config["intent_labels"]))
        mlflow.log_param("train_size", len(dataset))
        mlflow.log_param("val_size", len(val_dataset))
        mlflow.log_param("device", device)

        tokenizer = AutoTokenizer.from_pretrained(config["tokenizer"]["type"])

        # Pre-create label mappings for O(1) lookups (optimization)
        entity_labels = config["entity_labels"]
        id2label_ner = dict(enumerate(entity_labels))
        label2id_ner = {label: i for i, label in enumerate(entity_labels)}

        # Pre-detect best attention implementation once (avoid retry loop for 3x model loading)
        def detect_best_attention():
            """Detect best available attention implementation (Flash Attention 2 > SDPA > eager)"""
            if device != "cuda":
                return None  # CPU doesn't support Flash Attention

            # Quick test with minimal overhead
            try:
                from transformers import AutoConfig
                test_config = AutoConfig.from_pretrained(config["tokenizer"]["type"])
                test_config.num_labels = 2  # Minimal config
                # Try loading with Flash Attention 2
                AutoModelForTokenClassification.from_pretrained(
                    config["tokenizer"]["type"],
                    config=test_config,
                    attn_implementation="flash_attention_2",
                    torch_dtype=torch.float16
                )
                return "flash_attention_2"
            except Exception:
                try:
                    # Fallback to SDPA
                    AutoModelForTokenClassification.from_pretrained(
                        config["tokenizer"]["type"],
                        config=test_config,
                        attn_implementation="sdpa",
                        torch_dtype=torch.float16
                    )
                    return "sdpa"
                except Exception:
                    return None  # Will use default eager

        # Utiliser CRF si activ dans la config
        use_crf = config.get("training", {}).get("use_crf", False)

        # Detect attention once (not per model)
        best_attn = detect_best_attention() if not use_crf else None

        if use_crf:
            print("\n[CRF] CRF Layer active - Garantit des sequences BIO valides")
            from transformers import AutoConfig
            model_config = AutoConfig.from_pretrained(config["tokenizer"]["type"])
            model_config.num_labels = len(config["entity_labels"])
            model_config.id2label = id2label_ner
            model_config.label2id = label2id_ner

            # Note: Notre classe CRF custom ne supporte pas Flash Attention 2 ni SDPA
            # On utilise l'attention standard (eager) mais optimisée avec PyTorch compile
            model_config._attn_implementation = "eager"
            print("[INFO] Utilisation de l'attention standard (eager) - sera optimisee par PyTorch compile")

            model_ner = CamembertForTokenClassificationWithCRF.from_pretrained(
                config["tokenizer"]["type"],
                config=model_config,
                dtype=torch.float16 if device=="cuda" else torch.float32
            )
        else:
            # Use pre-detected attention (no retry loop - single model load)
            model_kwargs = {
                "num_labels": len(config["entity_labels"]),
                "id2label": id2label_ner,
                "label2id": label2id_ner,
                "torch_dtype": torch.float16 if device=="cuda" else torch.float32
            }
            if best_attn:
                model_kwargs["attn_implementation"] = best_attn

            model_ner = AutoModelForTokenClassification.from_pretrained(
                config["tokenizer"]["type"],
                **model_kwargs
            )

            if best_attn == "flash_attention_2":
                print("[OPTIM] Flash Attention 2 enabled")
            elif best_attn == "sdpa":
                print("[OPTIM] SDPA (PyTorch native) enabled - ~80% gain vs standard attention")
            else:
                print("[INFO] Using standard attention")

        model_ner.to(device)

        # PyTorch compile (requires Triton - Linux only)
        if opt_cfg.get("pytorch_compile", False) and hasattr(torch, 'compile') and device == "cuda":
            print("[OPTIMIZATION] Compiling NER model with PyTorch 2.x...")
            model_ner = torch.compile(model_ner, mode='reduce-overhead')
            print("[OK] NER model compiled")

        # gradient_checkpointing dsactiv pour plus de vitesse (CamemBERT-base tient en mmoire)

        def tokenize_and_align_labels_ner(examples):
            all_input = []
            for text, labels_bio in zip(examples["text"], examples["labels_bio"]):
                tokenized_inputs = tokenizer(
                    text,
                    truncation=True,
                    max_length=config["tokenizer"]["max_length"],
                    return_offsets_mapping=True,
                    is_split_into_words=False
                )
                offsets = tokenized_inputs["offset_mapping"]
                words = text.split()
                char_to_label = {}
                char_pos = 0
                for word_idx, word in enumerate(words):
                    word_start = text.find(word, char_pos)
                    if word_start == -1:
                        continue
                    word_end = word_start + len(word)
                    if word_idx < len(labels_bio):
                        for i in range(word_start, word_end):
                            char_to_label[i] = labels_bio[word_idx]
                    char_pos = word_end

                labels = []
                for start, end in offsets:
                    if start == end:
                        labels.append(-100)
                    else:
                        label_str = char_to_label.get(start, "O")
                        # Use pre-computed label2id dict for O(1) lookup instead of O(n) list.index()
                        labels.append(label2id_ner[label_str])

                tokenized_inputs.pop("offset_mapping")
                tokenized_inputs["labels"] = labels
                all_input.append(tokenized_inputs)
            # Merge les dicts du batch
            return {k: [dic[k] for dic in all_input] for k in all_input[0]}


        # Align mapping batch_size with training batch_size for optimal performance
        map_batch_size = train_cfg["batch_size"]

        # Dataset caching to avoid re-tokenization (30-60s speedup per run)
        CACHE_DIR = "nlu/data/.cache"
        ensure_dir(CACHE_DIR)
        cache_path_train = f"{CACHE_DIR}/tokenized_ner_train"
        cache_path_val = f"{CACHE_DIR}/tokenized_ner_val"

        if os.path.exists(cache_path_train) and os.path.exists(cache_path_val):
            print("[CACHE] Loading cached tokenized NER datasets...")
            tokenized_dataset_ner = load_from_disk(cache_path_train)
            tokenized_val_dataset_ner = load_from_disk(cache_path_val)
            print("[CACHE] Loaded from cache")
        else:
            print("[INFO] Tokenizing NER datasets (will be cached for future runs)...")
            tokenized_dataset_ner = dataset.map(
                tokenize_and_align_labels_ner,
                batched=True,
                batch_size=map_batch_size,
                num_proc=4,  # Multi-threading pour tokenisation
                remove_columns=dataset.column_names  # Retirer toutes les colonnes originales
            )
            tokenized_val_dataset_ner = val_dataset.map(
                tokenize_and_align_labels_ner,
                batched=True,
                batch_size=map_batch_size,
                num_proc=4,
                remove_columns=val_dataset.column_names  # Retirer toutes les colonnes originales
            )
            # Save to cache for next time
            tokenized_dataset_ner.save_to_disk(cache_path_train)
            tokenized_val_dataset_ner.save_to_disk(cache_path_val)
            print("[CACHE] Saved to cache for future runs")
        data_collator_ner = DataCollatorForTokenClassification(tokenizer, pad_to_multiple_of=8)

        # Fonction de calcul des mtriques pour NER (vectorized for performance)
        def compute_metrics_ner(pred):
            predictions, labels = pred
            predictions = np.argmax(predictions, axis=2)

            # Vectorized flatten and filter (much faster than Python loop)
            mask = labels != -100
            true_labels = labels[mask].flatten()
            pred_labels = predictions[mask].flatten()

            # Calculer les mtriques
            precision, recall, f1, _ = precision_recall_fscore_support(
                true_labels, pred_labels, average='weighted', zero_division=0
            )
            acc = accuracy_score(true_labels, pred_labels)

            return {
                'accuracy': acc,
                'f1': f1,
                'precision': precision,
                'recall': recall
            }

        training_args_ner = TrainingArguments(
            output_dir="./checkpoints/ner",  # Temporary checkpoint directory
            num_train_epochs=train_cfg["epochs"],
            per_device_train_batch_size=train_cfg["batch_size"],
            per_device_eval_batch_size=train_cfg["batch_size"],
            learning_rate=train_cfg["learning_rate"],
            weight_decay=train_cfg.get("weight_decay", 0.01),
            warmup_ratio=train_cfg.get("warmup_ratio", 0.1),
            lr_scheduler_type=train_cfg.get("lr_scheduler_type", "cosine"),
            logging_dir="./logs/ner",
            logging_steps=train_cfg.get("logging_steps", 100),
            eval_strategy=train_cfg.get("eval_strategy", "epoch"),
            save_strategy=train_cfg.get("save_strategy", "no"),
            load_best_model_at_end=False,
            metric_for_best_model="f1",
            greater_is_better=True,
            overwrite_output_dir=True,
            fp16=(device=="cuda" and opt_cfg.get("use_fp16", False)),  # FP16 if CUDA and explicitly enabled
            bf16=(device=="cuda" and opt_cfg.get("use_bf16", False)),
            gradient_accumulation_steps=train_cfg.get("gradient_accumulation_steps", 1),
            dataloader_num_workers=opt_cfg.get("dataloader_num_workers", 4),
            dataloader_prefetch_factor=opt_cfg.get("dataloader_prefetch_factor", 2),
            dataloader_pin_memory=opt_cfg.get("dataloader_pin_memory", True),
            optim="adamw_torch_fused" if (device=="cuda" and opt_cfg.get("use_fused_optimizer", True)) else "adamw_torch",
            remove_unused_columns=False,
            report_to="mlflow"
        )

        trainer_ner = Trainer(
            model=model_ner,
            args=training_args_ner,
            train_dataset=tokenized_dataset_ner,
            eval_dataset=tokenized_val_dataset_ner,  # Dataset de validation
            tokenizer=tokenizer,
            data_collator=data_collator_ner,
            compute_metrics=compute_metrics_ner  # Ajout des mtriques F1/Precision/Recall
        )

        print("\n" + "="*70)
        print("ENTRANEMENT DU MODLE NER")
        print("="*70)
        trainer_ner.train()

        # Evaluate NER model (metrics are automatically logged via report_to="mlflow")
        trainer_ner.evaluate()

        # Save NER model to MLflow immediately after training
        print("\n[INFO] Saving NER model to MLflow...")
        try:
            # Save model using save_pretrained for better compatibility with custom layers
            with tempfile.TemporaryDirectory() as tmp_dir:
                ner_model_path = os.path.join(tmp_dir, "ner_model")
                model_ner.save_pretrained(ner_model_path)
                tokenizer.save_pretrained(ner_model_path)

                # Log the model directory as artifact
                mlflow.log_artifacts(ner_model_path, artifact_path="ner_model")
                print("[OK] NER model saved to MLflow artifacts")

                # Try to register in Model Registry
                try:
                    model_uri = f"runs:/{mlflow.active_run().info.run_id}/ner_model"
                    mlflow.register_model(model_uri, "nlu-ner")
                    print("[OK] NER model registered in MLflow Model Registry")
                except Exception as reg_err:
                    print(f"[WARN] Could not register NER model in registry: {reg_err}")
        except Exception as e:
            print(f"[ERROR] Failed to save NER model: {e}")
            import traceback
            traceback.print_exc()
            raise  # Re-raise to make the failure visible

        print("\n[INFO] NER training completed and saved to MLflow")

        print("\n" + "="*70)
        print("ENTRANEMENT DU MODLE INTENT")
        print("="*70)

        # Crer les mappings intent
        id2label_intent = dict(enumerate(config["intent_labels"]))
        label2id_intent = {label: i for i, label in enumerate(config["intent_labels"])}

        # Use pre-detected attention (same as NER - no retry loop)
        model_kwargs_intent = {
            "num_labels": len(config["intent_labels"]),
            "id2label": id2label_intent,
            "label2id": label2id_intent,
            "torch_dtype": torch.float16 if device=="cuda" else torch.float32
        }
        if best_attn:
            model_kwargs_intent["attn_implementation"] = best_attn

        model_intent = AutoModelForSequenceClassification.from_pretrained(
            config["tokenizer"]["type"],
            **model_kwargs_intent
        )

        if best_attn == "flash_attention_2":
            print("[OPTIM] Flash Attention 2 enabled for Intent")
        elif best_attn == "sdpa":
            print("[OPTIM] SDPA enabled for Intent")
        else:
            print("[INFO] Using standard attention for Intent")

        model_intent.to(device)

        # PyTorch compile (requires Triton - Linux only)
        if opt_cfg.get("pytorch_compile", False) and hasattr(torch, 'compile') and device == "cuda":
            print("[OPTIMIZATION] Compiling Intent model with PyTorch 2.x...")
            model_intent = torch.compile(model_intent, mode='reduce-overhead')
            print("[OK] Intent model compiled")

        # gradient_checkpointing dsactiv pour plus de vitesse

        def tokenize_intent(examples):
            tokenized = tokenizer(
                examples["text"],
                truncation=True,
                max_length=config["tokenizer"]["max_length"]
                # Pas de padding ici, fait dynamiquement par le DataCollator (plus efficace)
            )
            tokenized["labels"] = [label2id_intent[intent] for intent in examples["intent"]]
            return tokenized

        # Align mapping batch_size with training batch_size
        # Dataset caching for Intent (same as NER)
        cache_path_train_intent = f"{CACHE_DIR}/tokenized_intent_train"
        cache_path_val_intent = f"{CACHE_DIR}/tokenized_intent_val"

        if os.path.exists(cache_path_train_intent) and os.path.exists(cache_path_val_intent):
            print("[CACHE] Loading cached tokenized Intent datasets...")
            tokenized_dataset_intent = load_from_disk(cache_path_train_intent)
            tokenized_val_dataset_intent = load_from_disk(cache_path_val_intent)
            print("[CACHE] Loaded from cache")
        else:
            print("[INFO] Tokenizing Intent datasets (will be cached for future runs)...")
            tokenized_dataset_intent = dataset.map(
                tokenize_intent,
                batched=True,
                batch_size=map_batch_size,
                num_proc=4,  # Multi-threading
                remove_columns=dataset.column_names  # Retirer toutes les colonnes originales
            )
            tokenized_val_dataset_intent = val_dataset.map(
                tokenize_intent,
                batched=True,
                batch_size=map_batch_size,
                num_proc=4,
                remove_columns=val_dataset.column_names  # Retirer toutes les colonnes originales
            )
            # Save to cache
            tokenized_dataset_intent.save_to_disk(cache_path_train_intent)
            tokenized_val_dataset_intent.save_to_disk(cache_path_val_intent)
            print("[CACHE] Saved to cache for future runs")

        # Fonction de calcul des mtriques pour Intent
        def compute_metrics_intent(pred):
            predictions, labels = pred
            predictions = np.argmax(predictions, axis=1)

            # Calculer les mtriques
            precision, recall, f1, _ = precision_recall_fscore_support(
                labels, predictions, average='weighted', zero_division=0
            )
            acc = accuracy_score(labels, predictions)

            return {
                'accuracy': acc,
                'f1': f1,
                'precision': precision,
                'recall': recall
            }

        training_args_intent = TrainingArguments(
            output_dir="./checkpoints/intent",  # Temporary checkpoint directory
            num_train_epochs=train_cfg["epochs"],
            per_device_train_batch_size=train_cfg["batch_size"],
            per_device_eval_batch_size=train_cfg["batch_size"],
            learning_rate=train_cfg["learning_rate"],
            weight_decay=train_cfg.get("weight_decay", 0.01),
            warmup_ratio=train_cfg.get("warmup_ratio", 0.1),
            lr_scheduler_type=train_cfg.get("lr_scheduler_type", "cosine"),
            logging_dir="./logs/intent",
            logging_steps=train_cfg.get("logging_steps", 100),
            eval_strategy=train_cfg.get("eval_strategy", "epoch"),
            save_strategy=train_cfg.get("save_strategy", "no"),
            load_best_model_at_end=False,
            metric_for_best_model="f1",
            greater_is_better=True,
            overwrite_output_dir=True,
            fp16=(device=="cuda" and opt_cfg.get("use_fp16", False)),  # FP16 if CUDA and explicitly enabled
            bf16=(device=="cuda" and opt_cfg.get("use_bf16", False)),
            gradient_accumulation_steps=train_cfg.get("gradient_accumulation_steps", 1),
            dataloader_num_workers=opt_cfg.get("dataloader_num_workers", 4),
            dataloader_prefetch_factor=opt_cfg.get("dataloader_prefetch_factor", 2),
            dataloader_pin_memory=opt_cfg.get("dataloader_pin_memory", True),
            optim="adamw_torch_fused" if (device=="cuda" and opt_cfg.get("use_fused_optimizer", True)) else "adamw_torch",
            remove_unused_columns=False,
            report_to="mlflow"
        )

        # Add DataCollator for Intent (was missing, causing slower batching)
        data_collator_intent = DataCollatorWithPadding(tokenizer)

        trainer_intent = Trainer(
            model=model_intent,
            args=training_args_intent,
            train_dataset=tokenized_dataset_intent,
            eval_dataset=tokenized_val_dataset_intent,  # Dataset de validation
            tokenizer=tokenizer,
            data_collator=data_collator_intent,
            compute_metrics=compute_metrics_intent  # Ajout des mtriques F1/Precision/Recall
        )

        trainer_intent.train()

        # Evaluate Intent model (metrics are automatically logged via report_to="mlflow")
        trainer_intent.evaluate()

        # Save Intent model to MLflow immediately after training
        print("\n[INFO] Saving Intent model to MLflow...")
        try:
            # Save model using save_pretrained for better compatibility
            with tempfile.TemporaryDirectory() as tmp_dir:
                intent_model_path = os.path.join(tmp_dir, "intent_model")
                model_intent.save_pretrained(intent_model_path)
                tokenizer.save_pretrained(intent_model_path)

                # Log the model directory as artifact
                mlflow.log_artifacts(intent_model_path, artifact_path="intent_model")
                print("[OK] Intent model saved to MLflow artifacts")

                # Try to register in Model Registry
                try:
                    model_uri = f"runs:/{mlflow.active_run().info.run_id}/intent_model"
                    mlflow.register_model(model_uri, "nlu-intent")
                    print("[OK] Intent model registered in MLflow Model Registry")
                except Exception as reg_err:
                    print(f"[WARN] Could not register Intent model in registry: {reg_err}")
        except Exception as e:
            print(f"[ERROR] Failed to save Intent model: {e}")
            import traceback
            traceback.print_exc()
            raise  # Re-raise to make the failure visible

        print("\n[INFO] Intent training completed and saved to MLflow")

        # Save minimal config files for model metadata (using temporary files)
        try:
            with tempfile.NamedTemporaryFile(mode='w', suffix='_ner_labels.json', delete=False) as f_ner:
                json.dump({"entity_labels": config["entity_labels"]}, f_ner, indent=2)
                ner_config_path = f_ner.name

            with tempfile.NamedTemporaryFile(mode='w', suffix='_intent_labels.json', delete=False) as f_intent:
                json.dump({"intent_labels": config["intent_labels"]}, f_intent, indent=2)
                intent_config_path = f_intent.name

            # Log config files to MLflow (models already registered above)
            mlflow.log_artifact(ner_config_path, artifact_path="ner_config")
            mlflow.log_artifact(intent_config_path, artifact_path="intent_config")
            print("[OK] Config files logged to MLflow")

            # Clean up temporary files
            os.unlink(ner_config_path)
            os.unlink(intent_config_path)
        except Exception as e:
            print(f"[WARN] Error logging config files: {e}")

        # Logger les mtriques de production (pour le Model Registry)
        mlflow.set_tag("model_type", "nlu")
        mlflow.set_tag("framework", "transformers")
        mlflow.set_tag("base_model", config["tokenizer"]["type"])
        mlflow.set_tag("use_crf", str(use_crf))
        mlflow.set_tag("training_status", "completed")

        print("[OK] Tous les artifacts enregistrs dans MLflow")

        print("\n" + "="*70)
        print("ENTRANEMENT TERMIN")
        print("="*70)

if __name__ == "__main__":
    freeze_support()
    main()
