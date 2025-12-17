"""
Augmentation : Abréviations courantes
"""

import random
from typing import List, Dict


# Abréviations courantes en français
ABBREVIATIONS = {
    "aller": ["all", "allé"],
    "je veux": ["jveux", "je vx", "jvx"],
    "veux": ["vx"],
    "trajet": ["traj"],
    "comment": ["cmt", "cmnt"],
    "train": ["trn"],
    "depuis": ["dps", "dpuis"],
    "passant": ["passan", "passa"],
    "arrivée": ["arrivé", "arriv"],
    "départ": ["dép", "depar"],
    "direction": ["dir"],
    "connexion": ["co", "connex"],
    "itinéraire": ["itin", "itinér"],
    "billet": ["bil"],
    "rejoindre": ["rjoindre", "rejoind"],
    "cherche": ["cherch"],
    "prendre": ["prend", "prendr"],
}


def augment_abbreviations(text: str, entities: Dict) -> List[tuple]:
    """
    Ajoute des abréviations courantes (sans toucher aux entités)

    Exemples:
    - "Je veux aller" -> "Jveux all"
    - "Comment aller" -> "Cmt aller"
    - "Trajet de Paris" -> "Traj de Paris"
    """
    variations = []

    # Ne pas modifier les noms de villes et prénoms
    protected_words = set()
    if entities.get("from"):
        for word in entities["from"].split():
            protected_words.add(word.lower())
    if entities.get("to"):
        for word in entities["to"].split():
            protected_words.add(word.lower())
    for via in entities.get("via", []):
        for word in via.split():
            protected_words.add(word.lower())
    if entities.get("person"):
        for word in entities["person"].split():
            protected_words.add(word.lower())

    # Appliquer les abréviations (max 2 par phrase pour rester naturel)
    text_lower = text.lower()
    modified_count = 0
    max_modifications = 2

    for phrase, abbrevs in ABBREVIATIONS.items():
        if phrase in text_lower and modified_count < max_modifications:
            # Vérifier que la phrase ne contient pas de mots protégés
            phrase_words = set(phrase.split())
            if not phrase_words.intersection(protected_words):
                if random.random() < 0.3:  # 30% de chance
                    abbrev = random.choice(abbrevs)
                    # Remplacer en conservant la casse du premier mot
                    if text.split()[0][0].isupper():
                        abbrev = abbrev.capitalize()
                    new_text = text.replace(phrase, abbrev, 1)
                    if new_text != text:
                        variations.append((new_text, entities.copy()))
                        modified_count += 1

    return variations
