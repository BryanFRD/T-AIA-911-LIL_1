"""
Augmentation : Variations de ponctuation
"""

from typing import List, Dict
import random


# Variations de ponctuation possibles
PUNCTUATION_VARIANTS = ["", ".", "!", "?", "...", " ?", " !", " ."]


def augment_punctuation(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Ajoute des variations de ponctuation

    Exemples:
    - "Je veux aller de Paris à Lyon"
    - "Je veux aller de Paris à Lyon."
    - "Je veux aller de Paris à Lyon!"
    - "Je veux aller de Paris à Lyon?"
    """
    variations = []
    
    if random.random() > probability:
        return variations

    for punct in PUNCTUATION_VARIANTS:
        # Supprimer toute ponctuation existante à la fin
        clean_text = text.rstrip(".,!?… ")
        new_text = clean_text + punct
        variations.append((new_text, entities.copy()))

    return variations
