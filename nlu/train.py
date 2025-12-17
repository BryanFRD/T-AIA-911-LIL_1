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

# === Détection GPU automatique ===
print("="*70)
print("CONFIGURATION SYSTÈME")
print("="*70)

# Informations PyTorch
print(f"PyTorch version: {torch.__version__}")
print(f"CUDA disponible: {torch.cuda.is_available()}")

if torch.cuda.is_available():
    print(f"CUDA version: {torch.version.cuda}")
    print(f"cuDNN version: {torch.backends.cudnn.version()}")
    print(f"Nombre de GPU: {torch.cuda.device_count()}")
    print(f"GPU actuel: {torch.cuda.current_device()}")
    print(f"Nom du GPU: {torch.cuda.get_device_name(0)}")
    print(f"Mémoire GPU totale: {torch.cuda.get_device_properties(0).total_memory / 1e9:.2f} GB")
    device = "cuda"
else:
    print("Aucun GPU CUDA détecté - utilisation du CPU")
    device = "cpu"

print(f"\nDevice sélectionné pour l'entraînement: {device.upper()}")
print("="*70 + "\n")

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

    # Le dataset généré contient déjà tokens, labels (BIO), et intent
    # On va reconstruire le texte à partir des tokens
    dataset = Dataset.from_list([{
        "text": " ".join(item["tokens"]),
        "intent": item["intent"],
        "labels_bio": item["labels"]  # Labels BIO pré-calculés
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

def tokenize_and_align_labels_ner(example):
    """
    Tokenize avec CamemBERT et aligne les labels BIO pré-calculés

    Les labels BIO sont déjà calculés au niveau des mots. Il faut les aligner
    avec les sous-tokens du tokenizer CamemBERT.
    """
    tokenized_inputs = tokenizer(
        example["text"],
        truncation=True,
        max_length=config["tokenizer"]["max_length"],
        return_offsets_mapping=True,
        is_split_into_words=False
    )

    offsets = tokenized_inputs["offset_mapping"]
    labels_bio = example["labels_bio"]

    # Reconstruire un mapping position → label BIO
    # Les labels_bio correspondent aux mots séparés par des espaces
    words = example["text"].split()

    # Créer un mapping: position dans le texte → label BIO
    char_to_label = {}
    char_pos = 0
    for word_idx, word in enumerate(words):
        # Trouver la position du mot dans le texte
        word_start = example["text"].find(word, char_pos)
        if word_start == -1:
            continue
        word_end = word_start + len(word)

        # Assigner le label à toutes les positions de ce mot
        if word_idx < len(labels_bio):
            for i in range(word_start, word_end):
                char_to_label[i] = labels_bio[word_idx]

        char_pos = word_end

    # Aligner les labels avec les tokens du tokenizer
    labels = []
    for start, end in offsets:
        if start == end:  # Token spécial (CLS, SEP, PAD)
            labels.append(-100)
        else:
            # Prendre le label du premier caractère du token
            label_str = char_to_label.get(start, "O")
            labels.append(config["entity_labels"].index(label_str))

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
    overwrite_output_dir=True,
    use_cpu=(device == "cpu")
)

trainer_ner = Trainer(
    model=model_ner,
    args=training_args_ner,
    train_dataset=tokenized_dataset_ner,
    tokenizer=tokenizer,
    data_collator=data_collator_ner
)

# Vérifier le device du modèle avant l'entraînement
print(f"\n[INFO] Modèle NER sur device: {next(model_ner.parameters()).device}")
print(f"[INFO] Nombre total de steps: {len(tokenized_dataset_ner) // config['training']['batch_size'] * config['training']['epochs']}")
print()

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
    overwrite_output_dir=True,
    use_cpu=(device == "cpu")
)

trainer_intent = Trainer(
    model=model_intent,
    args=training_args_intent,
    train_dataset=tokenized_dataset_intent,
    tokenizer=tokenizer
)

# Vérifier le device du modèle avant l'entraînement
print(f"\n[INFO] Modèle Intent sur device: {next(model_intent.parameters()).device}")
print(f"[INFO] Nombre total de steps: {len(tokenized_dataset_intent) // config['training']['batch_size'] * config['training']['epochs']}")
print()

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
