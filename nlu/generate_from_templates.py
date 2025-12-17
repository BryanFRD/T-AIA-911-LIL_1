"""
Génère un dataset d'entraînement à partir de templates et d'une liste de villes
"""

import json
import random
import re
from typing import List, Dict

# Importer l'augmenteur
try:
    from nlu.augments import NLUAugmenter
    AUGMENTATION_AVAILABLE = True
except ImportError:
    try:
        from augments import NLUAugmenter
        AUGMENTATION_AVAILABLE = True
    except ImportError:
        AUGMENTATION_AVAILABLE = False
        print("Warning: Module d'augmentation non disponible")

def load_templates(templates_file="nlu/data/templates.json") -> Dict:
    """Charge les templates"""
    with open(templates_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_cities(cities_file="nlu/data/cities.json") -> List[str]:
    """Charge la liste des villes"""
    with open(cities_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def get_placeholders(pattern: str) -> List[str]:
    """Extrait tous les placeholders d'un pattern"""
    return re.findall(r'\{([^}]+)\}', pattern)

def generate_from_template(pattern: str, cities: List[str], people: List[str]) -> Dict:
    """
    Génère un exemple à partir d'un template

    Returns:
        Dict avec 'text' et 'entities' (from, to, via, person)
    """
    placeholders = get_placeholders(pattern)

    # Déterminer les villes nécessaires
    from_city = None
    to_city = None
    via_cities = []
    person = None

    format_dict = {}
    used_cities = set()

    for placeholder in placeholders:
        if placeholder == "from_city":
            from_city = random.choice(cities)
            format_dict["from_city"] = from_city
            used_cities.add(from_city)
        elif placeholder == "to_city":
            available = [c for c in cities if c not in used_cities]
            to_city = random.choice(available)
            format_dict["to_city"] = to_city
            used_cities.add(to_city)
        elif placeholder.startswith("via_city"):
            available = [c for c in cities if c not in used_cities]
            via_city = random.choice(available)
            format_dict[placeholder] = via_city
            via_cities.append(via_city)
            used_cities.add(via_city)
        elif placeholder == "person":
            person = random.choice(people)
            format_dict["person"] = person
        elif placeholder == "city":
            # Pour les exemples négatifs
            city = random.choice(cities)
            format_dict["city"] = city

    text = pattern.format(**format_dict)

    return {
        "text": text,
        "entities": {
            "from": from_city or "",
            "to": to_city or "",
            "via": via_cities,
            "person": person or ""
        }
    }

def text_to_bio(text: str, entities: Dict) -> tuple:
    """
    Convertit un texte et ses entités en format BIO

    Returns:
        (tokens, bio_labels)
    """
    # Tokenization simple
    tokens = text.replace("?", "").replace("!", "").replace(".", "").replace(",", "").split()
    bio_labels = ["O"] * len(tokens)

    def tag_entity(entity_text: str, entity_type: str):
        """Tag une entité dans les tokens (insensible à la casse)"""
        if not entity_text:
            return

        entity_tokens = entity_text.split()

        # Chercher l'entité dans les tokens (comparaison insensible à la casse)
        for i in range(len(tokens) - len(entity_tokens) + 1):
            # Comparer en minuscules
            tokens_lower = [t.lower() for t in tokens[i:i+len(entity_tokens)]]
            entity_tokens_lower = [t.lower() for t in entity_tokens]

            if tokens_lower == entity_tokens_lower:
                bio_labels[i] = f"B-{entity_type}"
                for j in range(1, len(entity_tokens)):
                    bio_labels[i+j] = f"I-{entity_type}"
                break

    # Tagger les entités
    tag_entity(entities["from"], "FROM")
    tag_entity(entities["to"], "TO")
    for via in entities["via"]:
        tag_entity(via, "VIA")
    tag_entity(entities["person"], "PERSON")

    return tokens, bio_labels

def generate_dataset(templates_data: Dict, cities: List[str],
                     num_samples_per_template: int = 10) -> List[Dict]:
    """
    Génère le dataset complet

    Returns:
        Liste de dicts avec 'text', 'intent', 'entities'
    """
    dataset = []
    people = templates_data.get("people", [])

    print("="*70)
    print("GENERATION DE DATASET A PARTIR DE TEMPLATES")
    print("="*70)

    print(f"\nChargement des templates...")
    print(f"  {len(templates_data['templates'])} templates positifs")
    print(f"  {len(templates_data['negative_examples'])} templates negatifs")

    print(f"\nChargement de la liste des villes...")
    print(f"  {len(cities)} villes chargees")

    print(f"\nChargement de la liste des prenoms...")
    print(f"  {len(people)} prenoms charges")

    print("\n" + "="*70)

    # Exemples positifs (trajet)
    print("\nGeneration des exemples positifs...")
    for template in templates_data["templates"]:
        pattern = template["pattern"]
        intent = template["intent"]

        for _ in range(num_samples_per_template):
            example = generate_from_template(pattern, cities, people)
            example["intent"] = intent
            dataset.append(example)

    print(f"  {len(dataset)} exemples positifs generes")

    # Exemples négatifs (other)
    print("\nGeneration des exemples negatifs...")
    num_negative_samples = max(50, len(dataset) // 3)  # Environ 1/3 du dataset
    per_template = max(1, num_negative_samples // len(templates_data["negative_examples"]))

    negative_count_start = len(dataset)
    for template in templates_data["negative_examples"]:
        pattern = template["pattern"]
        intent = template["intent"]

        for _ in range(per_template):
            example = generate_from_template(pattern, cities, people)
            example["intent"] = intent
            dataset.append(example)

    print(f"  {len(dataset) - negative_count_start} exemples negatifs generes")

    # Mélanger
    random.shuffle(dataset)

    print("\n" + "="*70)

    return dataset

def save_dataset(dataset: List[Dict], output_file: str):
    """Sauvegarde le dataset au format JSON"""
    # Convertir en format avec tokens et BIO labels pour l'entraînement
    training_data = []

    for example in dataset:
        tokens, bio_labels = text_to_bio(example["text"], example["entities"])
        training_data.append({
            "tokens": tokens,
            "labels": bio_labels,
            "intent": example["intent"]
        })

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(training_data, f, ensure_ascii=False, indent=None, separators=(',', ':'))

    print(f"\nDataset sauvegarde dans: {output_file}")
    print(f"Total: {len(training_data)} exemples")

    # Statistiques
    intent_counts = {}
    via_count = 0
    person_count = 0

    for example in dataset:
        intent = example["intent"]
        intent_counts[intent] = intent_counts.get(intent, 0) + 1

        if example["entities"]["via"]:
            via_count += 1
        if example["entities"]["person"]:
            person_count += 1

    print("\nStatistiques:")
    for intent, count in intent_counts.items():
        print(f"  - Intent '{intent}': {count}")
    print(f"  - Avec VIA: {via_count}")
    print(f"  - Avec PERSON: {person_count}")

if __name__ == "__main__":
    # Charger les données
    templates_data = load_templates()
    cities = load_cities()

    # Générer le dataset
    dataset = generate_dataset(
        templates_data,
        cities,
        num_samples_per_template=30  # Augmenté de 20 à 30 pour plus de variété
    )

    # Appliquer l'augmentation si disponible
    if AUGMENTATION_AVAILABLE:
        print("\n" + "="*70)
        print("APPLICATION DE LA DATA AUGMENTATION")
        print("="*70)
        augmenter = NLUAugmenter()
        dataset = augmenter.augment_dataset(
            dataset,
            augmentation_factor=4.0  # Augmenté de 3.0 à 4.0 pour plus d'exemples
        )
        print("\n" + "="*70)

    # Sauvegarder
    save_dataset(dataset, "nlu/data/train_generated.json")
