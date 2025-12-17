import os
import json
from pathlib import Path
from transformers import (
    AutoTokenizer,
    AutoModelForTokenClassification,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    DataCollatorForTokenClassification,
    pipeline
)
from datasets import Dataset
import torch
from multiprocessing import freeze_support

def main():
    # === Détection GPU automatique ===
    print("="*70)
    print("CONFIGURATION SYSTÈME")
    print("="*70)

    print(f"PyTorch version: {torch.__version__}")
    print(f"CUDA disponible: {torch.cuda.is_available()}")
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"\nDevice sélectionné: {device.upper()}")
    print("="*70 + "\n")

    DEFAULT_MODEL = "camembert-base"
    CONFIG_FILE = "nlu/config.json"
    OUTPUT_DIR_NER = "nlu/models/model_ner"
    OUTPUT_DIR_INTENT = "nlu/models/model_intent"

    os.makedirs(OUTPUT_DIR_NER, exist_ok=True)
    os.makedirs(OUTPUT_DIR_INTENT, exist_ok=True)

    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE) as f:
            config = json.load(f)
    else:
        config = {
            "tokenizer": {"type": DEFAULT_MODEL, "max_length": 128},
            "training": {"epochs": 5, "batch_size": 16, "learning_rate": 5e-5},  # Augmenté de 8 à 16
            "entity_labels": ["O", "B-FROM", "I-FROM", "B-TO", "I-TO", "B-VIA", "I-VIA"],
            "intent_labels": ["trajet", "other"]
        }

    dataset_path = "nlu/data/train_generated.json"
    if os.path.exists(dataset_path):
        with open(dataset_path) as f:
            raw_data = json.load(f)
        full_dataset = Dataset.from_list([{
            "text": " ".join(item["tokens"]),
            "intent": item["intent"],
            "labels_bio": item["labels"]
        } for item in raw_data])
        print(f"Dataset chargé: {len(full_dataset)} exemples")

        # Split train/validation (90/10)
        split = full_dataset.train_test_split(test_size=0.1, seed=42)
        dataset = split["train"]
        val_dataset = split["test"]
        print(f"  Train: {len(dataset)} exemples")
        print(f"  Validation: {len(val_dataset)} exemples")
    else:
        print(f"ERREUR: {dataset_path} n'existe pas!")
        return

    tokenizer = AutoTokenizer.from_pretrained(config["tokenizer"]["type"])

    id2label_ner = dict(enumerate(config["entity_labels"]))
    label2id_ner = {label: i for i, label in enumerate(config["entity_labels"])}
    model_ner = AutoModelForTokenClassification.from_pretrained(
        config["tokenizer"]["type"],
        num_labels=len(config["entity_labels"]),
        id2label=id2label_ner,
        label2id=label2id_ner
    )
    model_ner.to(device)
    # gradient_checkpointing désactivé pour plus de vitesse (CamemBERT-base tient en mémoire)

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
                    labels.append(config["entity_labels"].index(label_str))

            tokenized_inputs.pop("offset_mapping")
            tokenized_inputs["labels"] = labels
            all_input.append(tokenized_inputs)
        # Merge les dicts du batch
        return {k: [dic[k] for dic in all_input] for k in all_input[0]}


    tokenized_dataset_ner = dataset.map(
        tokenize_and_align_labels_ner,
        batched=True,
        batch_size=64,  # Augmenté de 32 à 64
        num_proc=4  # Multi-threading pour tokenisation
    )
    tokenized_val_dataset_ner = val_dataset.map(
        tokenize_and_align_labels_ner,
        batched=True,
        batch_size=64,
        num_proc=4
    )
    data_collator_ner = DataCollatorForTokenClassification(tokenizer, pad_to_multiple_of=8)

    training_args_ner = TrainingArguments(
        output_dir=OUTPUT_DIR_NER,
        num_train_epochs=config["training"]["epochs"],
        per_device_train_batch_size=config["training"]["batch_size"],
        per_device_eval_batch_size=config["training"]["batch_size"],
        learning_rate=config["training"]["learning_rate"],
        logging_dir="./logs/ner",
        logging_steps=50,  # Réduit le logging pour accélérer
        eval_strategy="epoch",  # Évaluation à chaque epoch
        save_strategy="epoch",  # Sauvegarder le meilleur modèle
        load_best_model_at_end=True,  # Charger le meilleur modèle à la fin
        metric_for_best_model="eval_loss",  # Métrique pour best model
        greater_is_better=False,  # Plus petite loss = mieux
        overwrite_output_dir=True,
        fp16=(device=="cuda"),
        gradient_accumulation_steps=1,  # Réduit de 2 à 1 (batch_size doublé)
        dataloader_num_workers=2,  # Accélère chargement données
        dataloader_pin_memory=True  # Optimisation GPU
    )

    trainer_ner = Trainer(
        model=model_ner,
        args=training_args_ner,
        train_dataset=tokenized_dataset_ner,
        eval_dataset=tokenized_val_dataset_ner,  # Dataset de validation
        tokenizer=tokenizer,
        data_collator=data_collator_ner
    )

    print("\n" + "="*70)
    print("ENTRAÎNEMENT DU MODÈLE NER")
    print("="*70)
    trainer_ner.train()
    model_ner.save_pretrained(OUTPUT_DIR_NER)
    tokenizer.save_pretrained(OUTPUT_DIR_NER)

    with open(f"{OUTPUT_DIR_NER}/labels_config.json", "w") as f:
        json.dump({"entity_labels": config["entity_labels"]}, f, indent=2)

    print(f"\n[OK] Modèle NER sauvegardé dans {OUTPUT_DIR_NER}")

    print("\n" + "="*70)
    print("ENTRAÎNEMENT DU MODÈLE INTENT")
    print("="*70)

    # Créer les mappings intent
    id2label_intent = dict(enumerate(config["intent_labels"]))
    label2id_intent = {label: i for i, label in enumerate(config["intent_labels"])}

    model_intent = AutoModelForSequenceClassification.from_pretrained(
        config["tokenizer"]["type"],
        num_labels=len(config["intent_labels"]),
        id2label=id2label_intent,
        label2id=label2id_intent
    )
    model_intent.to(device)
    # gradient_checkpointing désactivé pour plus de vitesse

    def tokenize_intent(examples):
        tokenized = tokenizer(
            examples["text"],
            truncation=True,
            max_length=config["tokenizer"]["max_length"]
            # Pas de padding ici, fait dynamiquement par le DataCollator (plus efficace)
        )
        tokenized["labels"] = [label2id_intent[intent] for intent in examples["intent"]]
        return tokenized

    tokenized_dataset_intent = dataset.map(
        tokenize_intent,
        batched=True,
        batch_size=64,  # Augmenté de 32 à 64
        num_proc=4  # Multi-threading
    )
    tokenized_val_dataset_intent = val_dataset.map(
        tokenize_intent,
        batched=True,
        batch_size=64,
        num_proc=4
    )

    training_args_intent = TrainingArguments(
        output_dir=OUTPUT_DIR_INTENT,
        num_train_epochs=config["training"]["epochs"],
        per_device_train_batch_size=config["training"]["batch_size"],
        per_device_eval_batch_size=config["training"]["batch_size"],
        learning_rate=config["training"]["learning_rate"],
        logging_dir="./logs/intent",
        logging_steps=50,  # Réduit le logging
        eval_strategy="epoch",  # Évaluation à chaque epoch
        save_strategy="epoch",  # Sauvegarder le meilleur modèle
        load_best_model_at_end=True,  # Charger le meilleur modèle
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        overwrite_output_dir=True,
        fp16=(device=="cuda"),
        gradient_accumulation_steps=1,  # Réduit de 2 à 1
        dataloader_num_workers=2,  # Accélère chargement
        dataloader_pin_memory=True  # Optimisation GPU
    )

    trainer_intent = Trainer(
        model=model_intent,
        args=training_args_intent,
        train_dataset=tokenized_dataset_intent,
        eval_dataset=tokenized_val_dataset_intent,  # Dataset de validation
        tokenizer=tokenizer
    )

    trainer_intent.train()
    model_intent.save_pretrained(OUTPUT_DIR_INTENT)
    tokenizer.save_pretrained(OUTPUT_DIR_INTENT)

    with open(f"{OUTPUT_DIR_INTENT}/labels_config.json", "w") as f:
        json.dump({"intent_labels": config["intent_labels"]}, f, indent=2)

    print(f"\n[OK] Modèle Intent sauvegardé dans {OUTPUT_DIR_INTENT}")

    print("\n" + "="*70)
    print("ENTRAÎNEMENT TERMINÉ")
    print("="*70)

if __name__ == "__main__":
    freeze_support()
    main()
