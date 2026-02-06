import os
import sys
import json
import tempfile
import atexit
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
from multiprocessing import freeze_support, cpu_count
import numpy as np
from sklearn.metrics import precision_recall_fscore_support, accuracy_score

# Import NER model with CRF
sys.path.insert(0, str(Path(__file__).parent.parent))
from nlu.layers.ner_crf import CamembertForTokenClassificationWithCRF
from common.mlflow_utils import setup_mlflow, start_run, log_training_config, log_pytorch_model, end_run
from common.training_utils import detect_device, print_system_info, load_config, print_dataset_info, ensure_dir, get_machine_name, load_env

# Import MLflow after transformers to avoid conflicts
import mlflow

# Load environment variables from .env (AWS credentials, MLflow config)
load_env()


def export_model_to_onnx(model, tokenizer, output_path, task_type="token-classification", opset_version=17):
    """
    Export a HuggingFace model to ONNX format

    Args:
        model: HuggingFace model to export
        tokenizer: Tokenizer for creating dummy inputs
        output_path: Path to save the ONNX model
        task_type: "token-classification" or "sequence-classification"
        opset_version: ONNX opset version (default 17 for PyTorch 2.x compatibility)
    """
    try:
        from torch.onnx import export as onnx_export

        print(f"[INFO] Exporting model to ONNX (opset {opset_version})...")

        # Set model to eval mode
        model.eval()
        device = next(model.parameters()).device

        # Create dummy inputs
        dummy_text = "Je veux aller de Paris à Lyon"
        inputs = tokenizer(dummy_text, return_tensors="pt", padding=True, truncation=True, max_length=128)
        inputs = {k: v.to(device) for k, v in inputs.items()}

        # Dynamic axes for variable batch size and sequence length
        dynamic_axes = {
            "input_ids": {0: "batch_size", 1: "sequence_length"},
            "attention_mask": {0: "batch_size", 1: "sequence_length"},
            "logits": {0: "batch_size"}
        }

        if task_type == "token-classification":
            dynamic_axes["logits"][1] = "sequence_length"

        # Export to ONNX
        with torch.no_grad():
            onnx_export(
                model,
                (inputs["input_ids"], inputs["attention_mask"]),
                output_path,
                input_names=["input_ids", "attention_mask"],
                output_names=["logits"],
                dynamic_axes=dynamic_axes,
                opset_version=opset_version,
                do_constant_folding=True,
                export_params=True
            )

        print(f"[OK] ONNX model exported to {output_path}")

        # Verify ONNX model
        try:
            import onnx
            onnx_model = onnx.load(output_path)
            onnx.checker.check_model(onnx_model)
            print("[OK] ONNX model validation passed")
        except ImportError:
            print("[WARN] onnx package not installed, skipping validation")
        except Exception as e:
            print(f"[WARN] ONNX validation failed: {e}")

        # Apply ONNX Runtime graph optimizations (layer fusion, nop elimination, etc.)
        try:
            from onnxruntime.transformers import optimizer
            optimized = optimizer.optimize_model(output_path, model_type='bert', num_heads=12, hidden_size=768)
            optimized.save_model_to_file(output_path)
            print("[OK] ONNX model optimized with ORT graph passes (+20-40% inference speed)")
        except ImportError:
            print("[WARN] onnxruntime.transformers not available, skipping ONNX optimization")
        except Exception as e:
            print(f"[WARN] ONNX optimization failed (model still valid): {e}")

        return True

    except Exception as e:
        print(f"[ERROR] Failed to export ONNX model: {e}")
        import traceback
        traceback.print_exc()
        return False


def cleanup_mlflow():
    """Cleanup function to ensure MLflow run is terminated properly"""
    if mlflow.active_run():
        print("\n[CLEANUP] Terminating active MLflow run...")
        try:
            mlflow.set_tag("training_status", "interrupted")
            mlflow.end_run(status="KILLED")
            print("[OK] MLflow run terminated")
        except Exception as e:
            print(f"[WARN] Error during MLflow cleanup: {e}")


def main():
    # Register cleanup handler for unexpected exits
    atexit.register(cleanup_mlflow)

    # Note: We'll setup MLflow separately for NER and Intent
    # to use different experiments (nlu-ner and nlu-intent)

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

    # ========================================================================
    # NER TRAINING - Separate MLflow experiment
    # ========================================================================
    print("\n" + "="*70)
    print("SETUP NER EXPERIMENT")
    print("="*70)
    setup_mlflow("nlu-ner", enable_system_metrics=True)

    with start_run("ner-training", source_name=source_name):
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

        # Pre-detect best attention implementation without loading the model
        def detect_best_attention():
            """Detect best available attention implementation (Flash Attention 2 > SDPA > eager)"""
            if device != "cuda":
                return None  # CPU doesn't support Flash Attention

            # Check Flash Attention 2 support (just check if package is installed)
            try:
                import flash_attn
                return "flash_attention_2"
            except ImportError:
                pass

            # SDPA is available by default in PyTorch 2.x on CUDA
            if hasattr(torch.nn.functional, 'scaled_dot_product_attention'):
                return "sdpa"

            return None  # Will use default eager

        # Utiliser CRF si activ dans la config
        use_crf = config.get("training", {}).get("use_crf", False)

        # Detect attention once (not per model)
        best_attn = detect_best_attention() if not use_crf else None

        # CRITICAL: Always load model in FP32
        # Let Trainer handle mixed precision (fp16/bf16) automatically
        # This avoids gradient scaler conflicts and ensures proper autocast behavior
        model_dtype = torch.float32

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
                dtype=model_dtype
            )
        else:
            # Use pre-detected attention (no retry loop - single model load)
            model_kwargs = {
                "num_labels": len(config["entity_labels"]),
                "id2label": id2label_ner,
                "label2id": label2id_ner,
                "dtype": model_dtype
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


        # Tokenization is CPU-bound: use larger batch size than training for less overhead
        map_batch_size = 256
        map_num_proc = min(8, cpu_count() - 1)

        # Dataset caching to avoid re-tokenization (30-60s speedup per run)
        CACHE_DIR = "nlu/data/.cache"
        ensure_dir(CACHE_DIR)
        cache_path_train = f"{CACHE_DIR}/tokenized_ner_train"
        cache_path_val = f"{CACHE_DIR}/tokenized_ner_val"

        # Check if cache exists and try to load it
        cache_valid = False
        if os.path.exists(cache_path_train) and os.path.exists(cache_path_val):
            try:
                print("[CACHE] Loading cached tokenized NER datasets...")
                tokenized_dataset_ner = load_from_disk(cache_path_train)
                tokenized_val_dataset_ner = load_from_disk(cache_path_val)
                print("[CACHE] Loaded from cache")
                cache_valid = True
            except Exception as e:
                print(f"[WARN] Cache corrupted, will regenerate: {e}")
                # Delete corrupted cache
                import shutil
                if os.path.exists(cache_path_train):
                    shutil.rmtree(cache_path_train)
                if os.path.exists(cache_path_val):
                    shutil.rmtree(cache_path_val)

        if not cache_valid:
            print("[INFO] Tokenizing NER datasets (will be cached for future runs)...")
            tokenized_dataset_ner = dataset.map(
                tokenize_and_align_labels_ner,
                batched=True,
                batch_size=map_batch_size,
                num_proc=map_num_proc,
                remove_columns=dataset.column_names  # Retirer toutes les colonnes originales
            )
            tokenized_val_dataset_ner = val_dataset.map(
                tokenize_and_align_labels_ner,
                batched=True,
                batch_size=map_batch_size,
                num_proc=map_num_proc,
                remove_columns=val_dataset.column_names  # Retirer toutes les colonnes originales
            )
            # Save to cache for next time
            tokenized_dataset_ner.save_to_disk(cache_path_train)
            tokenized_val_dataset_ner.save_to_disk(cache_path_val)
            print("[CACHE] Saved to cache for future runs")
        data_collator_ner = DataCollatorForTokenClassification(tokenizer, pad_to_multiple_of=8)

        # Fonction de calcul des mtriques pour NER (vectorized for performance)
        def compute_metrics_ner(pred):
            # Handle both EvalPrediction object and tuple
            if hasattr(pred, 'predictions'):
                predictions = pred.predictions
                labels = pred.label_ids
            else:
                predictions, labels = pred

            # CRF retourne directement les prédictions (1D), pas les logits (2D)
            # Vérifier la dimension pour savoir si c'est du CRF ou non
            if isinstance(predictions, tuple):
                # Transformers sometimes returns (logits, ...) tuple
                predictions = predictions[0]

            if len(predictions.shape) == 3:  # (batch, seq_len, num_labels) - logits
                predictions = np.argmax(predictions, axis=2)
            # Sinon predictions est déjà (batch, seq_len) - prédictions du CRF

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
            max_grad_norm=1.0,  # CRITICAL: Gradient clipping pour éviter explosion avec CRF
            fp16=(device=="cuda" and opt_cfg.get("use_fp16", False)),
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
            processing_class=tokenizer,
            data_collator=data_collator_ner,
            compute_metrics=compute_metrics_ner  # Ajout des mtriques F1/Precision/Recall
        )

        print("\n" + "="*70)
        print("ENTRANEMENT DU MODLE NER")
        print("="*70)
        print(f"[DEBUG] Starting NER training with {len(tokenized_dataset_ner)} examples...")
        print(f"[DEBUG] Batch size: {train_cfg['batch_size']}, Gradient accumulation: {train_cfg.get('gradient_accumulation_steps', 1)}")
        print(f"[DEBUG] Effective batch size: {train_cfg['batch_size'] * train_cfg.get('gradient_accumulation_steps', 1)}")
        print(f"[DEBUG] Device: {device}, BF16: {opt_cfg.get('use_bf16', False)}, FP16: {opt_cfg.get('use_fp16', False)}")
        print(f"[DEBUG] Dataloader workers: {opt_cfg.get('dataloader_num_workers', 4)}")
        print(f"[DEBUG] First batch will be compiled/optimized - this may take 30-60 seconds...")
        sys.stdout.flush()  # Force flush to see logs immediately in CI

        trainer_ner.train()
        print("[DEBUG] NER training completed successfully")

        # Evaluate NER model (metrics are automatically logged via report_to="mlflow")
        trainer_ner.evaluate()

        # Save NER model to MLflow immediately after training
        print("\n[INFO] Saving NER model to MLflow...")
        tmp_dir = None
        try:
            export_formats = config.get("export", {}).get("formats", ["pytorch"])
            onnx_opset = config.get("export", {}).get("onnx_opset", 17)

            # Create temporary directory that we'll manage manually
            tmp_dir = tempfile.mkdtemp()

            # ONNX export
            if "onnx" in export_formats:
                onnx_path = os.path.join(tmp_dir, "ner_model.onnx")
                success = export_model_to_onnx(
                    model_ner,
                    tokenizer,
                    onnx_path,
                    task_type="token-classification",
                    opset_version=onnx_opset
                )
                if success:
                    # Load ONNX model and log to MLflow
                    import onnx
                    onnx_model = onnx.load(onnx_path)
                    mlflow.onnx.log_model(
                        onnx_model=onnx_model,
                        artifact_path="ner_model_onnx",
                        registered_model_name="nlu-ner-onnx"
                    )
                    print("[OK] NER model (ONNX) logged to MLflow")

                    # Also save raw ONNX file
                    mlflow.log_artifact(onnx_path, artifact_path="onnx_files")
                    print("[OK] NER ONNX file saved to MLflow")

            # Save tokenizer
            tokenizer_path = os.path.join(tmp_dir, "tokenizer")
            tokenizer.save_pretrained(tokenizer_path)
            mlflow.log_artifacts(tokenizer_path, artifact_path="ner_tokenizer")
            print("[OK] NER tokenizer saved to MLflow")

            # CRITICAL: Flush MLflow to ensure artifacts are uploaded before cleanup
            if mlflow.active_run():
                mlflow.flush_async_logging()
                print("[INFO] MLflow artifacts flushed")

        except Exception as e:
            print(f"[ERROR] Failed to save NER model: {e}")
            import traceback
            traceback.print_exc()
            raise  # Re-raise to make the failure visible
        finally:
            # Clean up temporary directory after MLflow has uploaded
            if tmp_dir and os.path.exists(tmp_dir):
                import shutil
                shutil.rmtree(tmp_dir)
                print("[INFO] Temporary directory cleaned up")

        print("\n[INFO] NER training completed and saved to MLflow")

        # Save NER labels config
        try:
            with tempfile.NamedTemporaryFile(mode='w', suffix='_ner_labels.json', delete=False) as f_ner:
                json.dump({"entity_labels": config["entity_labels"]}, f_ner, indent=2)
                ner_config_path = f_ner.name

            # Log config file to MLflow
            mlflow.log_artifact(ner_config_path, artifact_path="ner_config")
            print("[OK] NER config logged to MLflow")

            # Clean up temporary file
            os.unlink(ner_config_path)
        except Exception as e:
            print(f"[WARN] Error logging NER config: {e}")

        # Log NER-specific tags
        mlflow.set_tag("model_type", "nlu-ner")
        mlflow.set_tag("framework", "transformers")
        mlflow.set_tag("base_model", config["tokenizer"]["type"])
        mlflow.set_tag("use_crf", str(config["training"].get("use_crf", False)))
        mlflow.set_tag("training_status", "completed")

        print("[OK] NER artifacts enregistrs dans MLflow")

    # Free NER model memory before loading Intent model (-440MB VRAM)
    del model_ner, trainer_ner
    if device == "cuda":
        torch.cuda.empty_cache()
    print("[OPTIMIZATION] NER model freed from memory")

    # End NER run - start new Intent run
    print("\n" + "="*70)
    print("SETUP INTENT EXPERIMENT")
    print("="*70)
    setup_mlflow("nlu-intent", enable_system_metrics=True)

    with start_run("intent-training", source_name=source_name):
        # Log Intent-specific params
        log_training_config(config, device)
        mlflow.log_param("num_intent_labels", len(config["intent_labels"]))
        mlflow.log_param("train_size", len(dataset))
        mlflow.log_param("val_size", len(val_dataset))
        mlflow.log_param("device", device)

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
            "dtype": model_dtype
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

        # Dataset caching for Intent (same as NER)
        cache_path_train_intent = f"{CACHE_DIR}/tokenized_intent_train"
        cache_path_val_intent = f"{CACHE_DIR}/tokenized_intent_val"

        # Check if Intent cache exists and try to load it
        cache_valid_intent = False
        if os.path.exists(cache_path_train_intent) and os.path.exists(cache_path_val_intent):
            try:
                print("[CACHE] Loading cached tokenized Intent datasets...")
                tokenized_dataset_intent = load_from_disk(cache_path_train_intent)
                tokenized_val_dataset_intent = load_from_disk(cache_path_val_intent)
                print("[CACHE] Loaded from cache")
                cache_valid_intent = True
            except Exception as e:
                print(f"[WARN] Intent cache corrupted, will regenerate: {e}")
                # Delete corrupted cache
                import shutil
                if os.path.exists(cache_path_train_intent):
                    shutil.rmtree(cache_path_train_intent)
                if os.path.exists(cache_path_val_intent):
                    shutil.rmtree(cache_path_val_intent)

        if not cache_valid_intent:
            print("[INFO] Tokenizing Intent datasets (will be cached for future runs)...")
            tokenized_dataset_intent = dataset.map(
                tokenize_intent,
                batched=True,
                batch_size=map_batch_size,
                num_proc=map_num_proc,
                remove_columns=dataset.column_names  # Retirer toutes les colonnes originales
            )
            tokenized_val_dataset_intent = val_dataset.map(
                tokenize_intent,
                batched=True,
                batch_size=map_batch_size,
                num_proc=map_num_proc,
                remove_columns=val_dataset.column_names  # Retirer toutes les colonnes originales
            )
            # Save to cache
            tokenized_dataset_intent.save_to_disk(cache_path_train_intent)
            tokenized_val_dataset_intent.save_to_disk(cache_path_val_intent)
            print("[CACHE] Saved to cache for future runs")

        # Fonction de calcul des mtriques pour Intent
        def compute_metrics_intent(pred):
            # Handle both EvalPrediction object and tuple
            if hasattr(pred, 'predictions'):
                predictions = pred.predictions
                labels = pred.label_ids
            else:
                predictions, labels = pred

            # Handle tuple predictions (logits, ...)
            if isinstance(predictions, tuple):
                predictions = predictions[0]

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
            fp16=(device=="cuda" and opt_cfg.get("use_fp16", False)),
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
            processing_class=tokenizer,
            data_collator=data_collator_intent,
            compute_metrics=compute_metrics_intent  # Ajout des mtriques F1/Precision/Recall
        )

        trainer_intent.train()

        # Evaluate Intent model (metrics are automatically logged via report_to="mlflow")
        trainer_intent.evaluate()

        # Save Intent model to MLflow immediately after training
        print("\n[INFO] Saving Intent model to MLflow...")
        tmp_dir = None
        try:
            export_formats = config.get("export", {}).get("formats", ["pytorch"])
            onnx_opset = config.get("export", {}).get("onnx_opset", 17)

            # Create temporary directory that we'll manage manually
            tmp_dir = tempfile.mkdtemp()

            # ONNX export
            if "onnx" in export_formats:
                onnx_path = os.path.join(tmp_dir, "intent_model.onnx")
                success = export_model_to_onnx(
                    model_intent,
                    tokenizer,
                    onnx_path,
                    task_type="sequence-classification",
                    opset_version=onnx_opset
                )
                if success:
                    # Load ONNX model and log to MLflow
                    import onnx
                    onnx_model = onnx.load(onnx_path)
                    mlflow.onnx.log_model(
                        onnx_model=onnx_model,
                        artifact_path="intent_model_onnx",
                        registered_model_name="nlu-intent-onnx"
                    )
                    print("[OK] Intent model (ONNX) logged to MLflow")

                    # Also save raw ONNX file
                    mlflow.log_artifact(onnx_path, artifact_path="onnx_files")
                    print("[OK] Intent ONNX file saved to MLflow")

            # Save tokenizer
            tokenizer_path = os.path.join(tmp_dir, "tokenizer")
            tokenizer.save_pretrained(tokenizer_path)
            mlflow.log_artifacts(tokenizer_path, artifact_path="intent_tokenizer")
            print("[OK] Intent tokenizer saved to MLflow")

            # CRITICAL: Flush MLflow to ensure artifacts are uploaded before cleanup
            if mlflow.active_run():
                mlflow.flush_async_logging()
                print("[INFO] MLflow artifacts flushed")

        except Exception as e:
            print(f"[ERROR] Failed to save Intent model: {e}")
            import traceback
            traceback.print_exc()
            raise  # Re-raise to make the failure visible
        finally:
            # Clean up temporary directory after MLflow has uploaded
            if tmp_dir and os.path.exists(tmp_dir):
                import shutil
                shutil.rmtree(tmp_dir)
                print("[INFO] Temporary directory cleaned up")

        print("\n[INFO] Intent training completed and saved to MLflow")

        # Save Intent labels config
        try:
            with tempfile.NamedTemporaryFile(mode='w', suffix='_intent_labels.json', delete=False) as f_intent:
                json.dump({"intent_labels": config["intent_labels"]}, f_intent, indent=2)
                intent_config_path = f_intent.name

            # Log config file to MLflow
            mlflow.log_artifact(intent_config_path, artifact_path="intent_config")
            print("[OK] Intent config logged to MLflow")

            # Clean up temporary file
            os.unlink(intent_config_path)
        except Exception as e:
            print(f"[WARN] Error logging Intent config: {e}")

        # Logger les mtriques de production (pour le Model Registry)
        mlflow.set_tag("model_type", "nlu-intent")
        mlflow.set_tag("framework", "transformers")
        mlflow.set_tag("base_model", config["tokenizer"]["type"])
        mlflow.set_tag("training_status", "completed")

        print("[OK] Intent artifacts enregistrs dans MLflow")

    print("\n" + "="*70)
    print("ENTRANEMENT TERMIN - NER + INTENT")
    print("="*70)

if __name__ == "__main__":
    freeze_support()
    main()
