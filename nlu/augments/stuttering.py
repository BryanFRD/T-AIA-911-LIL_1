"""
Augmentation : Répétitions et bégaiement
"""

import random
from typing import List, Dict


def augment_stuttering(text: str, entities: Dict) -> List[tuple]:
    """
    Ajoute des répétitions et bégaiements naturels

    Exemples:
    - "Je veux aller" -> "Je je veux aller"
    - "Comment aller" -> "Comment com- comment aller"
    - "Trajet de Paris" -> "Tra- trajet de Paris"
    """
    variations = []

    words = text.split()
    if len(words) < 2:
        return variations

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

    # Répétition simple (15% de chance)
    if random.random() < 0.15:
        # Répéter un mot (généralement le premier)
        repeat_idx = 0
        if words[repeat_idx].lower() not in protected_words:
            new_words = words.copy()
            new_words.insert(repeat_idx, words[repeat_idx].lower())
            variations.append((" ".join(new_words), entities.copy()))

    # Bégaiement avec tiret (10% de chance)
    if random.random() < 0.10:
        stutter_idx = random.randint(0, min(2, len(words) - 1))
        word = words[stutter_idx]
        if word.lower() not in protected_words and len(word) > 3:
            # Prendre les 2-3 premières lettres
            stutter_part = word[:random.randint(2, 3)]
            new_words = words.copy()
            new_words[stutter_idx] = f"{stutter_part}- {word}"
            variations.append((" ".join(new_words), entities.copy()))

    # Répétition d'un mot au milieu (10% de chance)
    if random.random() < 0.10 and len(words) > 2:
        repeat_idx = random.randint(1, len(words) - 1)
        if words[repeat_idx].lower() not in protected_words:
            new_words = words.copy()
            new_words.insert(repeat_idx, words[repeat_idx].lower())
            variations.append((" ".join(new_words), entities.copy()))

    return variations
