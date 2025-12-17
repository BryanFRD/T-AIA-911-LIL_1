"""
Augmentation : Contexte narratif
"""

import random
from typing import List, Dict


# Préfixes narratifs possibles
NARRATIVE_PREFIXES = [
    "Alors",
    "Donc",
    "Du coup",
    "Bon",
    "Écoute",
    "Écoutez",
    "Voilà",
    "En fait",
    "Attends",
    "Attendez",
    "Dis-moi",
    "Dites-moi",
    "Salut",
    "Bonjour",
    "Excusez-moi",
    "Excuse-moi",
    "S'il vous plaît",
]

# Suffixes narratifs possibles
NARRATIVE_SUFFIXES = [
    "s'il vous plaît",
    "s'il te plaît",
    "merci",
    "voilà",
    "quoi",
    "tu vois",
    "vous voyez",
    "c'est tout",
    "merci d'avance",
    "d'accord",
    "ok",
]


def augment_narrative_context(text: str, entities: Dict) -> List[tuple]:
    """
    Ajoute un contexte narratif avant ou après le texte

    Exemples:
    - "Je veux aller de Paris à Lyon" -> "Alors je veux aller de Paris à Lyon"
    - "Comment aller à Lyon" -> "Écoute comment aller à Lyon s'il te plaît"
    - "Trajet Paris Lyon" -> "Bonjour trajet Paris Lyon merci"
    """
    variations = []

    # Ajouter un préfixe (25% de chance)
    if random.random() < 0.25:
        prefix = random.choice(NARRATIVE_PREFIXES)
        # Ajuster la casse du texte original si on ajoute un préfixe
        new_text = text[0].lower() + text[1:] if len(text) > 0 else text
        variations.append((f"{prefix} {new_text}", entities.copy()))

    # Ajouter un suffixe (25% de chance)
    if random.random() < 0.25:
        suffix = random.choice(NARRATIVE_SUFFIXES)
        variations.append((f"{text} {suffix}", entities.copy()))

    # Ajouter préfixe ET suffixe (15% de chance)
    if random.random() < 0.15:
        prefix = random.choice(NARRATIVE_PREFIXES)
        suffix = random.choice(NARRATIVE_SUFFIXES)
        new_text = text[0].lower() + text[1:] if len(text) > 0 else text
        variations.append((f"{prefix} {new_text} {suffix}", entities.copy()))

    return variations
