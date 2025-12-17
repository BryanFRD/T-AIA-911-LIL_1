"""
Augmentation : Inversions d'ordre des mots (naturelles en français)
"""

import random
from typing import List, Dict


def augment_word_order(text: str, entities: Dict, probability: float) -> List[tuple]:
    import random
    variations = []

    if random.random() > probability:
        return variations

    words = text.split()
    # Identifier les indices des mots protégés
    protected_words = set()
    for key in ["from", "to", "via", "person"]:
        val = entities.get(key)
        if val:
            if isinstance(val, list):
                protected_words.update(val)
            else:
                protected_words.update(val.split())

    # Séparer les mots protégés et non protégés
    protected_list = [w for w in words if w in protected_words]
    free_list = [w for w in words if w not in protected_words]

    # Mélanger les mots libres
    random.shuffle(free_list)

    # Recomposer
    new_text = " ".join(free_list + protected_list)
    if new_text != text:
        variations.append((new_text, entities.copy()))

    return variations
