"""
Augmentation : Contexte narratif
"""

import random
from typing import List, Dict


# Préfixes narratifs possibles - version enrichie
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
    "Tiens",
    "Tu sais",
    "Eh bien",
    "Voilà voilà",
]

# Suffixes narratifs possibles - version enrichie
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
    "hein",
    "tu comprends",
    "vous comprenez",
    "quoi donc",
    "alors voilà",
]



def augment_narrative_context(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Ajoute un contexte narratif avant ou après le texte

    Exemples:
    - "Je veux aller de Paris à Lyon" -> "Alors je veux aller de Paris à Lyon"
    - "Comment aller à Lyon" -> "Écoute comment aller à Lyon s'il te plaît"
    - "Trajet Paris Lyon" -> "Bonjour trajet Paris Lyon merci"
    """
    variations = []

    # Ajouter un préfixe (25% de chance)
    if random.random() < probability:
        prefix = random.choice(NARRATIVE_PREFIXES)
        # Ajuster la casse du texte original si on ajoute un préfixe
        if len(text) > 1:
            new_text = text[0].lower() + text[1:]
        else:
            new_text = text.lower()
        variations.append((f"{prefix} {new_text}", entities.copy()))

    # Ajouter un suffixe (25% de chance)
    if random.random() < probability:
        suffix = random.choice(NARRATIVE_SUFFIXES)
        variations.append((f"{text} {suffix}", entities.copy()))

    # Ajouter préfixe ET suffixe (15% de chance)
    if random.random() < probability / 2:
        prefix = random.choice(NARRATIVE_PREFIXES)
        suffix = random.choice(NARRATIVE_SUFFIXES)
        if len(text) > 1:
            new_text = text[0].lower() + text[1:]
        else:
            new_text = text.lower()
        variations.append((f"{prefix} {new_text} {suffix}", entities.copy()))

    return variations
