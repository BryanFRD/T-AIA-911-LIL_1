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

    # === Config et dataset ===
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
            "training": {"epochs": 5, "batch_size": 8, "learning_rate": 5e-5},
            "entity_labels": ["O", "B-FROM", "I-FROM", "B-TO", "I-TO", "B-VIA", "I-VIA"],
            "intent_labels": ["trajet", "other"]
        }

    dataset_path = "nlu/data/train_generated.json"
    if os.path.exists(dataset_path):
        with open(dataset_path) as f:
            raw_data = json.load(f)
        dataset = Dataset.from_list([{
            "text": " ".join(item["tokens"]),
            "intent": item["intent"],
            "labels_bio": item["labels"]
        } for item in raw_data])
        print(f"Dataset chargé: {len(dataset)} exemples")
    else:
        print(f"ERREUR: {dataset_path} n'existe pas!")
        return

    tokenizer = AutoTokenizer.from_pretrained(config["tokenizer"]["type"])

    # === NER ===
    id2label_ner = dict(enumerate(config["entity_labels"]))
    label2id_ner = {label: i for i, label in enumerate(config["entity_labels"])}
    model_ner = AutoModelForTokenClassification.from_pretrained(
        config["tokenizer"]["type"],
        num_labels=len(config["entity_labels"]),
        id2label=id2label_ner,
        label2id=label2id_ner
    )
    model_ner.to(device)
    model_ner.gradient_checkpointing_enable()

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
        batch_size=32,
        num_proc=1  # <-- mettre 1 sur Windows pour éviter les crashs
    )
    data_collator_ner = DataCollatorForTokenClassification(tokenizer, pad_to_multiple_of=8)

    training_args_ner = TrainingArguments(
        output_dir=OUTPUT_DIR_NER,
        num_train_epochs=config["training"]["epochs"],
        per_device_train_batch_size=config["training"]["batch_size"],
        learning_rate=config["training"]["learning_rate"],
        logging_dir="./logs/ner",
        logging_steps=10,
        save_strategy="epoch",
        overwrite_output_dir=True,
        fp16=(device=="cuda"),
        gradient_accumulation_steps=2
    )

    trainer_ner = Trainer(
        model=model_ner,
        args=training_args_ner,
        train_dataset=tokenized_dataset_ner,
        tokenizer=tokenizer,
        data_collator=data_collator_ner
    )

    trainer_ner.train()
    model_ner.save_pretrained(OUTPUT_DIR_NER)
    tokenizer.save_pretrained(OUTPUT_DIR_NER)
    print(f"[OK] Modèle NER sauvegardé dans {OUTPUT_DIR_NER}")

if __name__ == "__main__":
    freeze_support()  # nécessaire sur Windows pour multiprocessing
    main()
