"""
Augmentation : Mots de remplissage (hésitations naturelles)
"""

import random
from typing import List, Dict


# Mots de remplissage (fillers) français
FILLERS = [
    "euh", "hein", "ben", "bah", "alors", "donc", "voilà",
    "tu vois", "genre", "en fait", "quoi", "du coup",
    "t'sais", "quoi là", "hein là", "en gros", "si tu veux",
    "ok", "bon", "allez", "vois-tu"
]


def augment_fillers(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Ajoute des mots de remplissage (hésitations naturelles)

    Exemples:
    - "Euh je veux aller de Paris à Lyon"
    - "Je veux aller euh de Paris à Lyon"
    - "Je veux aller de Paris à Lyon hein"
    """
    variations = []

    # 20% de chance d'ajouter un filler au début
    if random.random() < probability:
        filler = random.choice(FILLERS)
        if len(text) > 1:
            new_text = f"{filler.capitalize()} {text[0].lower() + text[1:]}"
        else:
            new_text = f"{filler.capitalize()} {text.lower()}"
        variations.append((new_text, entities.copy()))

    # 15% de chance d'ajouter un filler au milieu
    if random.random() < 0.15:
        words = text.split()
        if len(words) > 3:
            insert_pos = random.randint(1, len(words) - 1)
            filler = random.choice(FILLERS)
            words.insert(insert_pos, filler)
            variations.append((" ".join(words), entities.copy()))

    # 15% de chance d'ajouter un filler à la fin
    if random.random() < 0.15:
        filler = random.choice(FILLERS)
        new_text = f"{text} {filler}"
        variations.append((new_text, entities.copy()))

    return variations
