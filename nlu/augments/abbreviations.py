"""
Augmentation : Abréviations courantes
"""

import random
from typing import List, Dict


# Abréviations courantes en français
ABBREVIATIONS = {
    "aller": ["all", "allé", "al"],
    "je veux": ["jveux", "je vx", "jvx", "je v"],
    "veux": ["vx", "v"],
    "trajet": ["traj", "trj"],
    "comment": ["cmt", "cmnt"],
    "train": ["trn", "tr"],
    "depuis": ["dps", "dpuis", "dep"],
    "passant": ["passan", "passa", "psnt"],
    "arrivée": ["arrivé", "arriv", "arr"],
    "départ": ["dép", "depar", "dep"],
    "direction": ["dir", "dirn"],
    "connexion": ["co", "connex", "cx"],
    "itinéraire": ["itin", "itinér", "itn"],
    "billet": ["bil", "blt"],
    "rejoindre": ["rjoindre", "rejoind", "rjdr"],
    "cherche": ["cherch", "chrc", "chch"],
    "prendre": ["prend", "prendr", "prnd"],
    "pour": ["pr"],
    "avec": ["avc"],
    "sans": ["sn"],
}

def augment_abbreviations(text: str, entities: Dict, probability: float, max_modifications: int = 2) -> List[tuple]:
    """
    Applique des abréviations courantes en français, en protégeant les entités.

    Args:
        text: phrase originale
        entities: dictionnaire des entités
        probability: probabilité d'appliquer une abréviation par occurrence
        max_modifications: nombre maximal de modifications par phrase

    Returns:
        Liste de tuples (texte_modifié, entités)
    """
    variations = []
    protected_words = set()

    for key in ["from", "to", "via", "person"]:
        if entities.get(key):
            if isinstance(entities[key], list):
                for item in entities[key]:
                    protected_words.update([w.lower() for w in item.split()])
            else:
                protected_words.update([w.lower() for w in entities[key].split()])

    text_lower = text.lower()
    modified_count = 0
    seen_texts = set()

    for phrase, abbrevs in ABBREVIATIONS.items():
        start = 0
        while start < len(text_lower) and modified_count < max_modifications:
            idx = text_lower.find(phrase, start)
            if idx == -1:
                break

            phrase_words = phrase.split()
            if not phrase_words or phrase_words[0] in protected_words:
                start = idx + 1
                continue

            if random.random() < probability:
                abbrev = random.choice(abbrevs)
                # Conserver la casse du premier caractère de la phrase à remplacer
                if text[idx].isupper():
                    abbrev = abbrev.capitalize()

                new_text = text[:idx] + abbrev + text[idx+len(phrase):]
                if new_text not in seen_texts:
                    variations.append((new_text, entities.copy()))
                    seen_texts.add(new_text)
                    modified_count += 1

            start = idx + len(phrase)

    return variations
