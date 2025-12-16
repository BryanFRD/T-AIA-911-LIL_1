"""
Augmentation : Inversions d'ordre des mots (naturelles en français)
"""

import random
from typing import List, Dict


def augment_word_order(text: str, entities: Dict) -> List[tuple]:
    """
    Génère des inversions d'ordre naturelles en français

    Exemples:
    - "De Paris à Lyon, je veux aller"
    - "À Lyon je veux aller depuis Paris"
    """
    variations = []

    # Extraction des parties de la phrase
    if entities.get("from") and entities.get("to"):
        from_city = entities["from"]
        to_city = entities["to"]

        # Différents ordres possibles
        patterns = [
            f"De {from_city} à {to_city} comment aller",
            f"Pour aller de {from_city} à {to_city}",
            f"Un trajet de {from_city} vers {to_city}",
            f"{to_city} depuis {from_city} comment y aller",
        ]

        for pattern in patterns:
            if random.random() < 0.3:  # 30% de chance
                variations.append((pattern, entities.copy()))

    return variations
