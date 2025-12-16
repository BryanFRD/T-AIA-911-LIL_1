"""
Génère un dataset d'entraînement à partir de templates et d'une liste de villes
"""

import json
import random
from pathlib import Path
from typing import List, Dict

def load_templates(templates_file="nlu/data/templates.json") -> Dict:
    """Charge les templates"""
    with open(templates_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_cities(cities_file="nlu/data/cities.json") -> List[str]:
    """Charge la liste des villes"""
    with open(cities_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_persons(persons_file="nlu/data/persons.json") -> List[str]:
    """Charge la liste des prénoms"""
    with open(persons_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def generate_dataset(templates_data: Dict, cities: List[str], persons: List[str],
                    num_samples_per_template: int = 10,
                    num_negative_samples: int = 20) -> List[Dict]:
    """
    Génère un dataset à partir des templates

    Args:
        templates_data: Dict contenant templates et negative_examples
        cities: Liste des villes
        persons: Liste des prénoms
        num_samples_per_template: Nombre d'exemples par template
        num_negative_samples: Nombre d'exemples négatifs à générer
    """
    dataset = []

    # 1. Générer les exemples positifs (trajets)
    print(f"\nGeneration des exemples positifs...")
    for template_info in templates_data["templates"]:
        pattern = template_info["pattern"]
        has_via = template_info.get("has_via", False)
        has_person = template_info.get("has_person", False)
        num_via = template_info.get("num_via", 1)  # Nombre de villes via (1 par défaut)

        for _ in range(num_samples_per_template):
            # Choisir des villes aléatoires
            from_city = random.choice(cities)
            to_city = random.choice([c for c in cities if c != from_city])

            # Déterminer le prénom (50% de chance d'être vide si has_person=True)
            if has_person:
                person_name = random.choice(persons) if random.random() > 0.5 else ""
            else:
                person_name = ""

            if has_via:
                # Choisir plusieurs villes via différentes
                used_cities = [from_city, to_city]
                via_cities = []
                for i in range(num_via):
                    available_cities = [c for c in cities if c not in used_cities]
                    if available_cities:
                        via_city = random.choice(available_cities)
                        via_cities.append(via_city)
                        used_cities.append(via_city)

                # Créer le dictionnaire de formatage
                format_dict = {
                    "from_city": from_city,
                    "to_city": to_city,
                    "person": person_name
                }

                # Ajouter les villes via avec leurs indices
                if num_via == 1:
                    format_dict["via_city"] = via_cities[0] if via_cities else ""
                else:
                    for i, via_city in enumerate(via_cities, 1):
                        format_dict[f"via_city{i}"] = via_city

                # Formater le texte
                if has_person:
                    if person_name:
                        text = pattern.format(**format_dict)
                    else:
                        # Supprimer les placeholders de personne
                        clean_pattern = pattern.replace("Dis {person} ", "").replace("Ok {person} ", "").replace("Hey {person} ", "").replace("{person} ", "")
                        text = clean_pattern.format(**{k: v for k, v in format_dict.items() if k != "person"})
                else:
                    text = pattern.format(**format_dict)

                via_list = via_cities
            else:
                # Vérifier si le pattern contient {via_city} (utilisé comme autre chose)
                if "{via_city}" in pattern:
                    via_city = random.choice([c for c in cities if c not in [from_city, to_city]])
                    if has_person:
                        text = pattern.format(from_city=from_city, to_city=to_city, via_city=via_city, person=person_name) if person_name else pattern.replace("Dis {person} ", "").replace("Ok {person} ", "").replace("Hey {person} ", "").replace("{person} ", "").format(from_city=from_city, to_city=to_city, via_city=via_city)
                    else:
                        text = pattern.format(from_city=from_city, to_city=to_city, via_city=via_city)
                else:
                    if has_person:
                        text = pattern.format(from_city=from_city, to_city=to_city, person=person_name) if person_name else pattern.replace("Dis {person} ", "").replace("Ok {person} ", "").replace("Hey {person} ", "").replace("{person} ", "").format(from_city=from_city, to_city=to_city)
                    else:
                        text = pattern.format(from_city=from_city, to_city=to_city)
                via_list = []

            example = {
                "text": text,
                "intent": template_info["intent"],
                "entities": {
                    "from": from_city,
                    "to": to_city,
                    "via": via_list,
                    "person": person_name
                }
            }
            dataset.append(example)

    print(f"  {len(dataset)} exemples positifs generes")

    # 2. Générer les exemples négatifs
    print(f"\nGeneration des exemples negatifs...")
    negative_count = 0
    for neg_template in templates_data["negative_examples"]:
        pattern = neg_template["pattern"]

        # Déterminer combien d'exemples générer pour ce template
        samples_for_this = num_negative_samples // len(templates_data["negative_examples"])

        for _ in range(samples_for_this):
            # Remplacer {city} si présent
            if "{city}" in pattern:
                city = random.choice(cities)
                text = pattern.format(city=city)
            else:
                text = pattern

            example = {
                "text": text,
                "intent": neg_template["intent"],
                "entities": {
                    "from": "",
                    "to": "",
                    "via": [],
                    "person": ""
                }
            }
            dataset.append(example)
            negative_count += 1

    print(f"  {negative_count} exemples negatifs generes")

    # Mélanger le dataset
    random.shuffle(dataset)

    return dataset

def save_dataset(dataset: List[Dict], output_file="nlu/data/train_generated.json"):
    """Sauvegarde le dataset"""
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)

    print(f"\nDataset sauvegarde dans: {output_file}")
    print(f"Total: {len(dataset)} exemples")

    # Statistiques
    intent_trajet = sum(1 for x in dataset if x["intent"] == "trajet")
    intent_other = sum(1 for x in dataset if x["intent"] == "other")
    with_via = sum(1 for x in dataset if len(x["entities"]["via"]) > 0)
    with_person = sum(1 for x in dataset if x["entities"].get("person", ""))

    print(f"\nStatistiques:")
    print(f"  - Intent 'trajet': {intent_trajet}")
    print(f"  - Intent 'other': {intent_other}")
    print(f"  - Avec VIA: {with_via}")
    print(f"  - Avec PERSON: {with_person}")

def show_sample(dataset: List[Dict], num_samples: int = 5):
    """Affiche quelques exemples"""
    print(f"\n{'='*70}")
    print(f"EXEMPLES GENERES ({num_samples} premiers)")
    print(f"{'='*70}")

    for i, example in enumerate(dataset[:num_samples], 1):
        print(f"\n{i}. [{example['intent']}] {example['text']}")
        if example['intent'] == 'trajet':
            print(f"   FROM: {example['entities']['from'] or '(vide)'}")
            print(f"   TO: {example['entities']['to'] or '(vide)'}")
            if example['entities']['via']:
                print(f"   VIA: {', '.join(example['entities']['via'])}")
            if example['entities'].get('person'):
                print(f"   PERSON: {example['entities']['person']}")

if __name__ == "__main__":
    print("="*70)
    print("GENERATION DE DATASET A PARTIR DE TEMPLATES")
    print("="*70)

    # Charger les données
    print("\nChargement des templates...")
    templates_data = load_templates()
    print(f"  {len(templates_data['templates'])} templates positifs")
    print(f"  {len(templates_data['negative_examples'])} templates negatifs")

    print("\nChargement de la liste des villes...")
    cities = load_cities()
    print(f"  {len(cities)} villes chargees")

    print("\nChargement de la liste des prenoms...")
    persons = load_persons()
    print(f"  {len(persons)} prenoms charges")

    # Générer le dataset
    print("\n" + "="*70)
    dataset = generate_dataset(
        templates_data,
        cities,
        persons,
        num_samples_per_template=10,  # 10 exemples par template
        num_negative_samples=100       # 100 exemples négatifs au total
    )

    # Sauvegarder
    print("\n" + "="*70)
    save_dataset(dataset)

    # Afficher quelques exemples
    show_sample(dataset, num_samples=10)
