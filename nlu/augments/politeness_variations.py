"""
Augmentation: Variations de politesse
Transforme les phrases pour ajouter ou modifier le niveau de politesse
"""

import random
import re
from typing import List, Dict, Tuple


# Transformations de verbes vers des formes polies
POLITENESS_TRANSFORMATIONS = [
    # (pattern, replacement)
    (r'\bje veux\b', ['je voudrais', 'j\'aimerais', 'je souhaiterais']),
    (r'\bje vais\b', ['je voudrais aller', 'j\'aimerais aller']),
    (r'\bdonne-moi\b', ['pourriez-vous me donner', 'pouvez-vous me donner', 'auriez-vous']),
    (r'\bmontre-moi\b', ['pourriez-vous me montrer', 'pouvez-vous me montrer']),
    (r'\bje prends\b', ['je prendrais', 'je souhaiterais prendre']),
    (r'\bc\'est combien\b', ['quel est le prix', 'combien coûte']),
    (r'\bfais-moi\b', ['pourriez-vous me faire', 'pouvez-vous me faire']),
]

# Phrases de politesse à ajouter en fin
POLITENESS_SUFFIXES = [
    's\'il vous plaît',
    'svp',
    'merci',
    's\'il te plaît',
    'stp',
    'merci beaucoup',
    'merci d\'avance',
]

# Transformations de l'impératif vers des questions polies
IMPERATIVE_TO_POLITE = [
    (r'^(donne|réserve|cherche|trouve|montre)', 'pourriez-vous \\1r'),
    (r'^(prends|prend)', 'pourriez-vous prendre'),
]


def add_politeness_suffix(text: str) -> str:
    """Ajoute un suffixe de politesse à la fin de la phrase"""
    # Don't add if already has politeness marker
    if any(marker in text.lower() for marker in ['s\'il vous plaît', 'svp', 'merci', 's\'il te plaît', 'stp']):
        return text

    suffix = random.choice(POLITENESS_SUFFIXES)

    # Remove ending punctuation if exists
    text = text.rstrip('!?.')

    # Add suffix with proper formatting
    return f"{text} {suffix}"


def transform_to_polite_form(text: str) -> str:
    """Transforme un verbe en forme polie"""
    for pattern, replacements in POLITENESS_TRANSFORMATIONS:
        if re.search(pattern, text, re.IGNORECASE):
            replacement = random.choice(replacements)
            text = re.sub(pattern, replacement, text, count=1, flags=re.IGNORECASE)
            # Capitalize first letter if it was at the start
            if text and text[0].islower() and pattern.startswith(r'\b'):
                text = text[0].upper() + text[1:]
            return text
    return text


def transform_imperative_to_polite(text: str) -> str:
    """Transforme un impératif en question polie"""
    for pattern, replacement in IMPERATIVE_TO_POLITE:
        if re.search(pattern, text, re.IGNORECASE):
            text = re.sub(pattern, replacement, text, count=1, flags=re.IGNORECASE)
            # Capitalize first letter
            if text:
                text = text[0].upper() + text[1:]
            return text
    return text


def augment_politeness_variations(text: str, entities: Dict, probability: float = 0.4) -> List[Tuple[str, Dict]]:
    """
    Applique des variations de politesse au texte

    Args:
        text: Texte original
        entities: Entités extraites
        probability: Probabilité d'appliquer cette transformation

    Returns:
        Liste de (texte_augmenté, entités)
    """
    if random.random() > probability:
        return []

    variants = []

    # Strategy 1: Add politeness suffix
    if random.random() < 0.5:
        new_text = add_politeness_suffix(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 2: Transform verb to polite form
    if random.random() < 0.5:
        new_text = transform_to_polite_form(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 3: Transform imperative to polite question
    if random.random() < 0.3:
        new_text = transform_imperative_to_polite(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 4: Combine polite form + suffix
    if random.random() < 0.3:
        new_text = transform_to_polite_form(text)
        new_text = add_politeness_suffix(new_text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    return variants[:2]  # Limit to 2 variants per example


if __name__ == "__main__":
    # Test
    test_examples = [
        "Je veux aller de Paris à Lyon",
        "Donne-moi un billet pour Lyon",
        "Je prends le train de 14h",
        "C'est combien pour aller à Lyon",
        "Réserve un train pour demain",
        "Montre-moi les horaires",
    ]

    for text in test_examples:
        print(f"\nOriginal: {text}")
        entities = {"from": "Paris", "to": "Lyon", "via": [], "voice": "", "time": "", "date": "", "transport": "train"}
        variants = augment_politeness_variations(text, entities, probability=1.0)
        for variant_text, _ in variants:
            print(f"  → {variant_text}")
