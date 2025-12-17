"""
Augmentation : Contractions orales françaises
"""

import random
import re
from typing import List, Dict


# Contractions orales courantes en français
CONTRACTIONS = {
    "je veux": ["j'veux", "chui", "j'aimerais", "jvoudrais"],
    "tu veux": ["tu veux", "t'veux", "tu voudrais", "tvoudrais"],
    "je suis": ["chui", "j'suis", "jsuis"],
    "il y a": ["y'a", "ya"],
    "nous allons": ["on va", "nous allons"],
    "je ne sais pas": ["j'sais pas", "chais pas", "je sais pas"],
    "il ne faut pas": ["faut pas", "il faut pas"],
    "je ne peux pas": ["j'peux pas", "peux pas", "jpeux pas"],
    "tu es": ["t'es", "t'es là"],
    "c'est": ["c'", "cest"],
    "il est": ["il est", "il est là", "il est bon"],  # utile pour contexte oral
    "elle est": ["elle est", "elle est là", "elle est bonne"],
    "on est": ["on est", "on est là", "on est prêt"],
}


def augment_contractions(text: str, entities: Dict, probability: float) -> List[tuple]:
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
                if random.random() < probability:  # 30% de chance
                    new_text = re.sub(
                        pattern,
                        replacement,
                        text,
                        flags=re.IGNORECASE
                    )
                    if new_text != text:
                        variations.append((new_text, entities.copy()))

    return variations
