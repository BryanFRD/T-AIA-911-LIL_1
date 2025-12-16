"""
Augmentation : Contractions orales françaises
"""

import random
import re
from typing import List, Dict


# Contractions orales courantes en français
CONTRACTIONS = {
    "je veux": ["j'veux", "chui", "j'aimerais"],
    "tu veux": ["tu veux", "t'veux"],
    "je suis": ["chui", "j'suis", "jsuis"],
    "il y a": ["y'a", "ya"],
}


def augment_contractions(text: str, entities: Dict) -> List[tuple]:
    """
    Ajoute des contractions orales courantes

    Exemples:
    - "J'veux aller de Paris à Lyon"
    - "Chu parti de Paris pour Lyon"
    """
    variations = []

    text_lower = text.lower()
    for pattern, replacements in CONTRACTIONS.items():
        if pattern in text_lower:
            for replacement in replacements:
                if random.random() < 0.3:  # 30% de chance
                    new_text = re.sub(
                        pattern,
                        replacement,
                        text,
                        flags=re.IGNORECASE
                    )
                    if new_text != text:
                        variations.append((new_text, entities.copy()))

    return variations
