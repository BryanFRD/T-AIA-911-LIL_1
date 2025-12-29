"""
Augmentation: Questions implicites
Transforme des affirmations en questions ou ajoute des marqueurs interrogatifs
"""

import random
import re
from typing import List, Dict, Tuple


# Transformations d'affirmations vers questions
STATEMENT_TO_QUESTION = [
    # (pattern, replacement)
    (r'^je veux aller (.*)', r'c\'est possible d\'aller \1 ?'),
    (r'^je veux (.*)', r'je peux avoir \1 ?'),
    (r'^je cherche (.*)', r'y\'a \1 ?'),
    (r'^un train (.*)', r'y\'a un train \1 ?'),
    (r'^un (taxi|avion|bus|car) (.*)', r'y\'a un \1 \2 ?'),
    (r'^(.*) pour (.*)$', r'\1 pour \2 ?'),
    (r'^aller (de|à) (.*)', r'c\'est possible d\'aller \1 \2 ?'),
]

# Préfixes pour transformer en question
QUESTION_PREFIXES = [
    'Est-ce qu\'il y a ',
    'Y\'a ',
    'C\'est possible ',
    'Je peux avoir ',
    'Tu as ',
    'Vous avez ',
]

# Suffixes pour ajouter un ton interrogatif
QUESTION_SUFFIXES = [
    ' ?',
    ' ou pas ?',
    ' ou non ?',
]


def add_question_mark(text: str) -> str:
    """Ajoute un point d'interrogation si pas déjà présent"""
    text = text.rstrip('.!?')
    return f"{text} ?"


def transform_to_question(text: str) -> str:
    """Transforme une affirmation en question"""
    # Try pattern-based transformations first
    for pattern, replacement in STATEMENT_TO_QUESTION:
        if re.search(pattern, text, re.IGNORECASE):
            new_text = re.sub(pattern, replacement, text, count=1, flags=re.IGNORECASE)
            # Capitalize first letter
            if new_text:
                new_text = new_text[0].upper() + new_text[1:]
            return new_text

    # If no pattern matches, try adding a prefix
    if random.random() < 0.3:
        prefix = random.choice(QUESTION_PREFIXES)
        # Lowercase first letter of original text
        text_lower = text[0].lower() + text[1:] if text else text
        return f"{prefix}{text_lower} ?"

    return text


def add_implicit_doubt(text: str) -> str:
    """Ajoute un suffixe de doute/question"""
    if '?' in text:
        return text  # Already a question

    suffix = random.choice(QUESTION_SUFFIXES)
    text = text.rstrip('.!?')
    return f"{text}{suffix}"


def simplify_to_keywords(text: str, entities: Dict) -> str:
    """
    Simplifie en format keywords (ex: "Paris Lyon ?" au lieu de phrase complète)
    Seulement pour les trajets simples avec FROM et TO
    """
    from_city = entities.get("from", "")
    to_city = entities.get("to", "")

    if from_city and to_city:
        # Simple keyword format
        variations = [
            f"{from_city} {to_city} ?",
            f"{from_city} {to_city}",
            f"{to_city} ?",
        ]
        return random.choice(variations)

    return text


def augment_implicit_questions(text: str, entities: Dict, probability: float = 0.2) -> List[Tuple[str, Dict]]:
    """
    Transforme des affirmations en questions ou ajoute des marqueurs interrogatifs

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

    # Strategy 1: Just add question mark
    if random.random() < 0.4:
        new_text = add_question_mark(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 2: Transform to explicit question
    if random.random() < 0.4:
        new_text = transform_to_question(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 3: Add implicit doubt
    if random.random() < 0.3:
        new_text = add_implicit_doubt(text)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    # Strategy 4: Simplify to keywords (low probability)
    if random.random() < 0.15:
        new_text = simplify_to_keywords(text, entities)
        if new_text != text:
            variants.append((new_text, entities.copy()))

    return variants[:2]  # Limit to 2 variants per example


if __name__ == "__main__":
    # Test
    test_examples = [
        ("Je veux aller de Paris à Lyon", {"from": "Paris", "to": "Lyon"}),
        ("Je cherche un train pour Lyon", {"from": "", "to": "Lyon"}),
        ("Un train demain", {"from": "", "to": "", "date": "demain"}),
        ("Train Paris Lyon", {"from": "Paris", "to": "Lyon"}),
        ("Je veux un billet pour Lyon", {"from": "", "to": "Lyon"}),
        ("Aller de Paris à Lyon", {"from": "Paris", "to": "Lyon"}),
    ]

    for text, entities in test_examples:
        print(f"\nOriginal: {text}")
        full_entities = {"from": "", "to": "", "via": [], "voice": "", "time": "", "date": "", "transport": "train"}
        full_entities.update(entities)
        variants = augment_implicit_questions(text, full_entities, probability=1.0)
        for variant_text, _ in variants:
            print(f"  → {variant_text}")
