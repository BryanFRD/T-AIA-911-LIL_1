"""
Génère un dataset d'entraînement à partir de templates et d'une liste de villes
"""

import json
import random
import re
from typing import List, Dict, Tuple, Set
from multiprocessing import Pool, cpu_count
from functools import partial

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

def load_cities(cities_file="nlu/data/cities.json") -> Tuple[Set[str], List[str]]:
    """Charge la liste des villes et retourne un set pour O(1) lookups"""
    with open(cities_file, 'r', encoding='utf-8') as f:
        cities_list = json.load(f)
        return set(cities_list), cities_list  # Return both set for lookups and list for sampling

def get_placeholders(pattern: str) -> List[str]:
    """Extrait tous les placeholders d'un pattern"""
    return re.findall(r'\{([^}]+)\}', pattern)

def generate_from_template(pattern: str, cities: List[str], voices: List[str], times: List[str] = None, dates: List[str] = None, transports: List[str] = None, departure_times: List[str] = None, arrival_times: List[str] = None) -> Dict:
    """
    Génère un exemple à partir d'un template

    Returns:
        Dict avec 'text' et 'entities' (from, to, via, voice, time, date, transport, departure_time, arrival_time)
    """
    placeholders = get_placeholders(pattern)

    # Déterminer les villes nécessaires
    from_city = None
    to_city = None
    via_cities = []
    voice = None
    time = None
    date = None
    transport = "train"  # Valeur par défaut
    departure_time = None
    arrival_time = None

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
        elif placeholder == "voice":
            voice = random.choice(voices)
            format_dict["voice"] = voice
        elif placeholder == "time":
            if times:
                time = random.choice(times)
                format_dict["time"] = time
        elif placeholder == "date":
            if dates:
                date = random.choice(dates)
                format_dict["date"] = date
        elif placeholder == "departure_time":
            if departure_times:
                departure_time = random.choice(departure_times)
                format_dict["departure_time"] = departure_time
        elif placeholder == "arrival_time":
            if arrival_times:
                arrival_time = random.choice(arrival_times)
                format_dict["arrival_time"] = arrival_time
        elif placeholder == "transport":
            if transports:
                transport = random.choice(transports)
                format_dict["transport"] = transport
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
            "voice": voice or "",
            "time": time or "",
            "date": date or "",
            "departure_time": departure_time or "",
            "arrival_time": arrival_time or "",
            "transport": transport
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

    # Pre-compute lowercase tokens once (optimization: avoid recreating in loop)
    tokens_lower = [t.lower() for t in tokens]

    def tag_entity(entity_text: str, entity_type: str):
        """Tag une entité dans les tokens (insensible à la casse)"""
        if not entity_text:
            return

        entity_tokens = entity_text.split()
        entity_tokens_lower = [t.lower() for t in entity_tokens]
        entity_len = len(entity_tokens)

        # Chercher l'entité dans les tokens (comparaison insensible à la casse)
        for i in range(len(tokens) - entity_len + 1):
            # Compare with pre-computed lowercase tokens (O(n) instead of O(n²))
            if tokens_lower[i:i+entity_len] == entity_tokens_lower:
                bio_labels[i] = f"B-{entity_type}"
                for j in range(1, entity_len):
                    bio_labels[i+j] = f"I-{entity_type}"
                break

    # Tagger les entités
    tag_entity(entities["from"], "FROM")
    tag_entity(entities["to"], "TO")
    for via in entities["via"]:
        tag_entity(via, "VIA")
    tag_entity(entities.get("voice", ""), "VOICE")
    tag_entity(entities.get("time", ""), "TIME")
    tag_entity(entities.get("date", ""), "DATE")
    tag_entity(entities.get("departure_time", ""), "DEPARTURE_TIME")
    tag_entity(entities.get("arrival_time", ""), "ARRIVAL_TIME")
    tag_entity(entities.get("transport", ""), "TRANSPORT")

    return tokens, bio_labels

def _generate_example_with_intent(pattern: str, intent: str, cities: List[str], voices: List[str], times: List[str] = None, dates: List[str] = None, transports: List[str] = None, departure_times: List[str] = None, arrival_times: List[str] = None) -> Dict:
    """
    Helper function for parallel generation.
    Generates a single example from template with intent.
    """
    example = generate_from_template(pattern, cities, voices, times, dates, transports, departure_times, arrival_times)
    example["intent"] = intent
    return example


def generate_dataset(templates_data: Dict, cities: List[str],
                     num_samples_per_template: int = 10) -> List[Dict]:
    """
    Génère le dataset complet

    Returns:
        Liste de dicts avec 'text', 'intent', 'entities'
    """
    dataset = []
    voices = templates_data.get("voices", [])
    times = templates_data.get("times", [])
    dates = templates_data.get("dates", [])
    transports = templates_data.get("transports", [])
    departure_times = templates_data.get("departure_times", [])
    arrival_times = templates_data.get("arrival_times", [])

    print("="*70)
    print("GENERATION DE DATASET A PARTIR DE TEMPLATES")
    print("="*70)

    print(f"\nChargement des templates...")
    print(f"  {len(templates_data['templates'])} templates positifs")
    print(f"  {len(templates_data['negative_examples'])} templates negatifs")

    print(f"\nChargement de la liste des villes...")
    print(f"  {len(cities)} villes chargees")

    print(f"\nChargement de la liste des voix...")
    print(f"  {len(voices)} voix chargees")

    print(f"\nChargement de la liste des heures...")
    print(f"  {len(times)} heures chargees")

    print(f"\nChargement de la liste des dates...")
    print(f"  {len(dates)} dates chargees")

    print(f"\nChargement de la liste des transports...")
    print(f"  {len(transports)} transports charges")

    print(f"\nChargement de la liste des heures de départ...")
    print(f"  {len(departure_times)} heures de départ chargees")

    print(f"\nChargement de la liste des heures d'arrivée...")
    print(f"  {len(arrival_times)} heures d'arrivée chargees")

    print("\n" + "="*70)

    # Exemples positifs (trajet) - PARALLELIZED for 4-6x speedup
    print("\nGeneration des exemples positifs (parallel)...")

    # Prepare tasks: (pattern, intent) tuples repeated num_samples_per_template times
    positive_tasks = []
    for template in templates_data["templates"]:
        for _ in range(num_samples_per_template):
            positive_tasks.append((template["pattern"], template["intent"], cities, voices, times, dates, transports, departure_times, arrival_times))

    # Use multiprocessing Pool for parallel generation
    num_workers = min(6, cpu_count())
    with Pool(processes=num_workers) as pool:
        positive_examples = pool.starmap(_generate_example_with_intent, positive_tasks)

    dataset.extend(positive_examples)
    print(f"  {len(positive_examples)} exemples positifs generes (using {num_workers} workers)")

    # Exemples négatifs (other) - PARALLELIZED
    print("\nGeneration des exemples negatifs (parallel)...")
    num_negative_samples = max(50, len(dataset) // 3)  # Environ 1/3 du dataset
    per_template = max(1, num_negative_samples // len(templates_data["negative_examples"]))

    negative_tasks = []
    for template in templates_data["negative_examples"]:
        for _ in range(per_template):
            negative_tasks.append((template["pattern"], template["intent"], cities, voices, times, dates, transports, departure_times, arrival_times))

    negative_count_start = len(dataset)
    with Pool(processes=num_workers) as pool:
        negative_examples = pool.starmap(_generate_example_with_intent, negative_tasks)

    dataset.extend(negative_examples)

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
    voice_count = 0
    transport_count = 0
    transport_explicit_count = 0

    for example in dataset:
        intent = example["intent"]
        intent_counts[intent] = intent_counts.get(intent, 0) + 1

        if example["entities"]["via"]:
            via_count += 1
        if example["entities"]["voice"]:
            voice_count += 1
        if example["entities"]["transport"]:
            transport_count += 1
            # Check if transport is explicitly mentioned in text (not just default "train")
            if example["entities"]["transport"] in example["text"].lower():
                transport_explicit_count += 1

    print("\nStatistiques:")
    for intent, count in intent_counts.items():
        print(f"  - Intent '{intent}': {count}")
    print(f"  - Avec VIA: {via_count}")
    print(f"  - Avec VOICE: {voice_count}")
    print(f"  - Avec TRANSPORT: {transport_count} (dont {transport_explicit_count} explicites)")

if __name__ == "__main__":
    # Charger les données
    templates_data = load_templates()
    cities_set, cities_list = load_cities()

    # Générer le dataset (use list for random.choice)
    dataset = generate_dataset(
        templates_data,
        cities_list,
        num_samples_per_template=30  # Augmenté de 20 à 30 pour plus de variété
    )

    # Appliquer l'augmentation si disponible (séparément pour trajet et other)
    if AUGMENTATION_AVAILABLE:
        print("\n" + "="*70)
        print("APPLICATION DE LA DATA AUGMENTATION")
        print("="*70)
        augmenter = NLUAugmenter()

        # Séparer trajet et other
        trajet_examples = [ex for ex in dataset if ex["intent"] == "trajet"]
        other_examples = [ex for ex in dataset if ex["intent"] == "other"]

        print(f"\nAvant augmentation:")
        print(f"  - trajet: {len(trajet_examples)} exemples")
        print(f"  - other: {len(other_examples)} exemples")
        print(f"  - Ratio: {len(trajet_examples)/len(other_examples):.1f}:1")

        # Augmenter avec des facteurs différents
        augmented_trajet = augmenter.augment_dataset(
            trajet_examples,
            augmentation_factor=4.0  # Fort pour trajet
        )
        augmented_other = augmenter.augment_dataset(
            other_examples,
            augmentation_factor=4.5  # Ajusté pour ratio ~3:1 (était 2.0, puis 6.0)
        )

        # Combiner
        dataset = augmented_trajet + augmented_other

        print(f"\nAprès augmentation:")
        print(f"  - trajet: {len(augmented_trajet)} exemples")
        print(f"  - other: {len(augmented_other)} exemples")
        print(f"  - Ratio: {len(augmented_trajet)/len(augmented_other):.1f}:1 (cible: 3:1)")
        print("\n" + "="*70)

    # Sauvegarder
    save_dataset(dataset, "nlu/data/train_generated.json")
