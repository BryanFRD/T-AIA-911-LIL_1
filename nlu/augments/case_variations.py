"""
Augmentation : Variations de casse
"""

import random
from typing import List, Dict


def augment_case_variations(text: str, entities: Dict) -> List[tuple]:
    """
    Génère des variations de casse

    Exemples:
    - "je veux aller de paris à lyon"
    - "Je Veux Aller De Paris À Lyon"
    - "JE VEUX ALLER DE PARIS À LYON"
    """
    variations = []

    # Tout en minuscules
    variations.append((text.lower(), entities.copy()))

    # Première lettre en majuscule
    variations.append((text.capitalize(), entities.copy()))

    # Tout en majuscules (rare mais possible dans certains contextes)
    if random.random() < 0.1:  # 10% de chance
        variations.append((text.upper(), entities.copy()))

    # Majuscule après ponctuation
    if "?" in text or "!" in text:
        words = text.split()
        for i, word in enumerate(words):
            if i > 0 and words[i-1].endswith(("?", "!", ".")):
                words[i] = word.capitalize()
        variations.append((" ".join(words), entities.copy()))

    return variations
