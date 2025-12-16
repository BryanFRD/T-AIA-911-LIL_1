"""
Augmentation : Fautes de frappe courantes
"""

import random
from typing import List, Dict


# Fautes de frappe courantes (patterns)
TYPO_RULES = {
    "aller": ["alé", "allé", "allez"],
    "veux": ["veu", "veut"],
    "de": ["dé", "de"],
    "à": ["a", "à"],
    "comment": ["comen", "comman"],
    "trajet": ["trajé", "trajais"],
}


def augment_typos(text: str, entities: Dict) -> List[tuple]:
    """
    Introduit des fautes de frappe courantes (sans toucher aux entités)

    Exemples:
    - "Je veu aller de Paris à Lyon"
    - "Je veux alé de Paris à Lyon"
    """
    variations = []

    # Ne pas modifier les noms de villes et prénoms
    protected_words = set()
    if entities.get("from"):
        protected_words.add(entities["from"].lower())
    if entities.get("to"):
        protected_words.add(entities["to"].lower())
    for via in entities.get("via", []):
        protected_words.add(via.lower())
    if entities.get("person"):
        protected_words.add(entities["person"].lower())

    words = text.split()
    modified = False

    for word_idx, word in enumerate(words):
        word_lower = word.lower().strip(".,!?")

        # Vérifier si le mot est protégé
        if word_lower in protected_words:
            continue

        # Appliquer les règles de typo
        if word_lower in TYPO_RULES:
            if random.random() < 0.3:  # 30% de chance de faute
                typo_variant = random.choice(TYPO_RULES[word_lower])
                # Conserver la casse originale
                if word[0].isupper():
                    typo_variant = typo_variant.capitalize()
                words[word_idx] = word.replace(word_lower, typo_variant, 1)
                modified = True

    if modified:
        variations.append((" ".join(words), entities.copy()))

    return variations
