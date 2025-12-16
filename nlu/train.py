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

# === Config par défaut ===
DEFAULT_MODEL = "camembert-base"
CONFIG_FILE = "nlu/config.json"
OUTPUT_DIR_NER = "nlu/models/model_ner"
OUTPUT_DIR_INTENT = "nlu/models/model_intent"

# Créer dossiers modèles si inexistants
os.makedirs(OUTPUT_DIR_NER, exist_ok=True)
os.makedirs(OUTPUT_DIR_INTENT, exist_ok=True)

# Charger config si existante, sinon créer config par défaut
if os.path.exists(CONFIG_FILE):
    with open(CONFIG_FILE) as f:
        config = json.load(f)
else:
    config = {
        "tokenizer": {
            "type": DEFAULT_MODEL,
            "max_length": 128
        },
        "training": {
            "epochs": 5,
            "batch_size": 8,
            "learning_rate": 5e-5
        },
        "entity_labels": ["O", "B-FROM", "I-FROM", "B-TO", "I-TO", "B-VIA", "I-VIA"],
        "intent_labels": ["trajet", "other"]
    }

# === Dataset ===
dataset_path = "nlu/data/train_generated.json"

if os.path.exists(dataset_path):
    with open(dataset_path) as f:
        raw_data = json.load(f)
    dataset = Dataset.from_list([{
        "text": item["text"],
        "intent": item["intent"],
        "entities": item["entities"]
    } for item in raw_data])
    print(f"Dataset charge: {dataset_path} ({len(dataset)} exemples)")
else:
    print(f"ERREUR: {dataset_path} n'existe pas!")
    print("Executez d'abord: python nlu/generate_from_templates.py")
    exit(1)

# === Tokenizer ===
tokenizer = AutoTokenizer.from_pretrained(config["tokenizer"]["type"])

# === 1. Modèle NER (Named Entity Recognition) ===
print("=== Entraînement du modèle NER ===")

# Créer le mapping id2label et label2id pour NER
id2label_ner = dict(enumerate(config["entity_labels"]))
label2id_ner = {label: i for i, label in enumerate(config["entity_labels"])}

model_ner = AutoModelForTokenClassification.from_pretrained(
    config["tokenizer"]["type"],
    num_labels=len(config["entity_labels"]),
    id2label=id2label_ner,
    label2id=label2id_ner
)

def find_entity_spans(text, entity_value):
    """Trouve les indices de début et fin d'une entité dans le texte"""
    if not entity_value:
        return []
    text_lower = text.lower()
    entity_lower = entity_value.lower()
    start = text_lower.find(entity_lower)
    if start == -1:
        return []
    return [(start, start + len(entity_lower))]

def get_token_label(start, end, offsets, entity_spans_dict):
    """Détermine le label BIO d'un token donné"""
    for entity_type, spans in entity_spans_dict.items():
        for span_start, span_end in spans:
            if start >= span_start and end <= span_end:
                # Vérifier si c'est le début de l'entité (B-) ou la suite (I-)
                offset_idx = offsets.index((start, end))
                is_begin = (start == span_start) or (offset_idx > 0 and offsets[offset_idx - 1][1] < span_start)
                prefix = "B-" if is_begin else "I-"
                return prefix + entity_type
    return "O"

def tokenize_and_align_labels_ner(example):
    """Tokenize et aligne les labels BIO pour NER"""
    tokenized_inputs = tokenizer(
        example["text"],
        truncation=True,
        max_length=config["tokenizer"]["max_length"],
        return_offsets_mapping=True
    )

    offsets = tokenized_inputs["offset_mapping"]

    # Collecter tous les spans d'entités
    from_spans = find_entity_spans(example["text"], example["entities"].get("from", ""))
    to_spans = find_entity_spans(example["text"], example["entities"].get("to", ""))
    via_spans = []
    for via_item in example["entities"].get("via", []):
        via_spans.extend(find_entity_spans(example["text"], via_item))

    entity_spans_dict = {
        "FROM": from_spans,
        "TO": to_spans,
        "VIA": via_spans
    }

    # Créer les labels pour chaque token
    labels = []
    for start, end in offsets:
        if start == end:  # Token spécial
            labels.append(-100)
        else:
            label = get_token_label(start, end, offsets, entity_spans_dict)
            labels.append(config["entity_labels"].index(label))

    tokenized_inputs.pop("offset_mapping")
    tokenized_inputs["labels"] = labels
    return tokenized_inputs

tokenized_dataset_ner = dataset.map(tokenize_and_align_labels_ner, batched=False)

data_collator_ner = DataCollatorForTokenClassification(tokenizer)

training_args_ner = TrainingArguments(
    output_dir=OUTPUT_DIR_NER,
    num_train_epochs=config["training"]["epochs"],
    per_device_train_batch_size=config["training"]["batch_size"],
    learning_rate=config["training"]["learning_rate"],
    logging_dir="./logs/ner",
    logging_steps=10,
    save_strategy="epoch",
    overwrite_output_dir=True
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

print(f"[OK] Modele NER sauvegarde dans {OUTPUT_DIR_NER}")

# === 2. Modèle Intent Classification ===
print("\n=== Entrainement du modele Intent ===")

# Créer le mapping id2label et label2id
id2label_intent = dict(enumerate(config["intent_labels"]))
label2id_intent = {label: i for i, label in enumerate(config["intent_labels"])}

model_intent = AutoModelForSequenceClassification.from_pretrained(
    config["tokenizer"]["type"],
    num_labels=len(config["intent_labels"]),
    id2label=id2label_intent,
    label2id=label2id_intent
)

def tokenize_intent(example):
    """Tokenize pour la classification d'intent"""
    tokenized = tokenizer(
        example["text"],
        truncation=True,
        max_length=config["tokenizer"]["max_length"],
        padding="max_length"
    )
    tokenized["labels"] = config["intent_labels"].index(example["intent"])
    return tokenized

tokenized_dataset_intent = dataset.map(tokenize_intent, batched=False)

training_args_intent = TrainingArguments(
    output_dir=OUTPUT_DIR_INTENT,
    num_train_epochs=config["training"]["epochs"],
    per_device_train_batch_size=config["training"]["batch_size"],
    learning_rate=config["training"]["learning_rate"],
    logging_dir="./logs/intent",
    logging_steps=10,
    save_strategy="epoch",
    overwrite_output_dir=True
)

trainer_intent = Trainer(
    model=model_intent,
    args=training_args_intent,
    train_dataset=tokenized_dataset_intent,
    tokenizer=tokenizer
)

trainer_intent.train()
model_intent.save_pretrained(OUTPUT_DIR_INTENT)
tokenizer.save_pretrained(OUTPUT_DIR_INTENT)

print(f"[OK] Modele Intent sauvegarde dans {OUTPUT_DIR_INTENT}")

# Sauvegarder la config
Path(OUTPUT_DIR_NER, "labels_config.json").write_text(json.dumps(config, indent=2))
Path(OUTPUT_DIR_INTENT, "labels_config.json").write_text(json.dumps(config, indent=2))

print("\n[OK] Entrainement termine !")

# === Fonction pour tester les prédictions ===
def extract_entities_from_ner(ner_result):
    """Extrait et regroupe les entités du résultat NER"""
    entities = {"FROM": [], "TO": [], "VIA": []}
    for r in ner_result:
        entity_type = r.get("entity_group", r.get("entity")).replace("B-", "").replace("I-", "")
        word = r["word"].replace("▁", " ").strip()
        if entity_type in entities:
            entities[entity_type].append(word)
    return entities

def print_entities(entities):
    """Affiche les entités extraites"""
    if entities["FROM"]:
        print(f"      FROM: {' '.join(entities['FROM'])}")
    if entities["TO"]:
        print(f"      TO: {' '.join(entities['TO'])}")
    if entities["VIA"]:
        print(f"      VIA: {', '.join(entities['VIA'])}")

def test_predictions(texts):
    """Teste le modèle NLU complet sur des exemples"""
    print("\n" + "="*60)
    print("TEST DES PRÉDICTIONS")
    print("="*60)

    # Pipeline NER
    ner_pipeline = pipeline(
        "token-classification",
        model=OUTPUT_DIR_NER,
        tokenizer=OUTPUT_DIR_NER,
        aggregation_strategy="simple"
    )

    # Pipeline Intent
    intent_pipeline = pipeline(
        "text-classification",
        model=OUTPUT_DIR_INTENT,
        tokenizer=OUTPUT_DIR_INTENT
    )

    for text in texts:
        print(f"\n> Texte : {text}")

        # Prédire l'intent
        intent_result = intent_pipeline(text)[0]
        print(f"  Intent : {intent_result['label']} (confiance: {intent_result['score']:.2%})")

        # Prédire les entités
        ner_result = ner_pipeline(text)
        if ner_result:
            print("  Entites :")
            entities = extract_entities_from_ner(ner_result)
            print_entities(entities)
        else:
            print("  Pas d'entites detectees")

# Exemples de test
example_texts = [
    "Je veux aller de Paris à Lyon",
    "Je veux aller de Marseille à Nice en passant par Cannes",
    "Comment me rendre à Port-Boulet depuis la gare de Tours ?",
    "Je veux faire Saint Denis Saint Etienne",
    "Aller de Lille à Strasbourg en passant par Reims"
]

test_predictions(example_texts)
