"""
Augmentation : Synonymes contextuels
"""

import random
from typing import List, Dict


# Synonymes pour variété
SYNONYMS = {
    "aller": ["me rendre", "voyager", "partir"],
    "veux": ["voudrais", "souhaite", "aimerais", "desire"],
    "comment": ["comment faire pour", "quel moyen pour", "quelle route pour"],
    "trajet": ["voyage", "itinéraire", "route", "parcours"],
}


def augment_synonyms(text: str, entities: Dict) -> List[tuple]:
    """
    Remplace certains mots par des synonymes

    Exemples:
    - "Je voudrais aller de Paris à Lyon"
    - "Je souhaite me rendre de Paris à Lyon"
    """
    variations = []

    words = text.split()

    for word_idx, word in enumerate(words):
        word_lower = word.lower().strip(".,!?")

        if word_lower in SYNONYMS:
            if random.random() < 0.4:  # 40% de chance de remplacement
                synonym = random.choice(SYNONYMS[word_lower])
                # Conserver la casse
                if word[0].isupper():
                    synonym = synonym.capitalize()

                new_words = words.copy()
                new_words[word_idx] = word.replace(word_lower, synonym, 1)
                variations.append((" ".join(new_words), entities.copy()))

    return variations
