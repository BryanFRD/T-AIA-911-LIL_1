"""
Augmentation : Variantes d'accents
Retire aléatoirement des accents pour simuler saisies sans accents
"""

import random
from typing import List, Dict

# Mapping des caractères accentués vers non-accentués
ACCENT_MAP = {
    'é': 'e', 'è': 'e', 'ê': 'e', 'ë': 'e',
    'à': 'a', 'â': 'a', 'ä': 'a',
    'ù': 'u', 'û': 'u', 'ü': 'u',
    'ô': 'o', 'ö': 'o',
    'î': 'i', 'ï': 'i',
    'ç': 'c',
    'œ': 'oe', 'æ': 'ae',
    # Majuscules
    'É': 'E', 'È': 'E', 'Ê': 'E', 'Ë': 'E',
    'À': 'A', 'Â': 'A', 'Ä': 'A',
    'Ù': 'U', 'Û': 'U', 'Ü': 'U',
    'Ô': 'O', 'Ö': 'O',
    'Î': 'I', 'Ï': 'I',
    'Ç': 'C',
    'Œ': 'OE', 'Æ': 'AE',
}

def augment_accent_variants(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Retire aléatoirement des accents pour simuler utilisateurs ne mettant pas d'accents

    Args:
        text: Texte à augmenter
        entities: Entités à préserver
        probability: Probabilité de retirer un accent (appliqué à chaque caractère accentué)

    Examples:
        - "Je veux aller de Paris à Lyon" -> "Je veux aller de Paris a Lyon"
        - "Trajet rapide" -> "Trajet rapide" (pas d'accents retirés)
        - "Réservation pour demain" -> "Reservation pour demain"

    Returns:
        Liste de tuples (texte_augmenté, entités)
    """
    variations = []

    # Ne pas modifier les noms de villes et prénoms (protéger entités)
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
    result_words = []

    for word in words:
        word_lower = word.lower().strip(".,!?")

        # Si le mot est protégé, on ne le modifie pas
        if word_lower in protected_words:
            result_words.append(word)
            continue

        # Retirer aléatoirement des accents dans ce mot
        new_word = ""
        for char in word:
            if char in ACCENT_MAP:
                if random.random() < probability:
                    new_word += ACCENT_MAP[char]
                    modified = True
                else:
                    new_word += char
            else:
                new_word += char

        result_words.append(new_word)

    if modified:
        variations.append((" ".join(result_words), entities.copy()))

    return variations
