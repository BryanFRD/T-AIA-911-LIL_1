"""
Augmentation : Transformations orales (contractions + abréviations)
Fusionne contractions.py et abbreviations.py pour éviter la redondance
"""

import random
import re
from typing import List, Dict


# Contractions orales (langage parlé, style SMS)
ORAL_CONTRACTIONS = {
    "je veux": ["j'veux", "jveux", "je vx", "jvx"],
    "tu veux": ["t'veux", "tveux"],
    "je suis": ["chui", "j'suis", "jsuis"],
    "tu es": ["t'es"],
    "il y a": ["y'a", "ya"],
    "nous allons": ["on va"],
    "je ne sais pas": ["j'sais pas", "chais pas", "je sais pas"],
    "il ne faut pas": ["faut pas", "il faut pas"],
    "je ne peux pas": ["j'peux pas", "peux pas", "jpeux pas"],
}

# Abréviations écrites (style SMS, chat)
TEXT_ABBREVIATIONS = {
    "comment": ["cmt", "cmnt"],
    "trajet": ["traj", "trj"],
    "depuis": ["dps", "dpuis"],
    "pour": ["pr"],
    "avec": ["avc"],
    "sans": ["sn"],
    "train": ["trn"],
    "itinéraire": ["itin", "itn"],
    "billet": ["bil", "blt"],
    "direction": ["dir"],
    "arrivée": ["arriv", "arr"],
    "départ": ["dép", "dep"],
    "cherche": ["cherch", "chrc"],
    "prendre": ["prend", "prnd"],
    "rejoindre": ["rjoindre", "rjdr"],
}


def augment_oral_transformations(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Applique des transformations orales (contractions OU abréviations, pas les deux)

    Stratégie:
    - 40% de chance → contractions orales (ex: "je veux" → "jveux")
    - 40% de chance → abréviations texte (ex: "comment" → "cmt")
    - 20% de chance → aucune transformation

    Protège les entités (FROM, TO, VIA, PERSON)

    Exemples:
    - "Je veux aller de Paris à Lyon" → "Jveux aller de Paris à Lyon"
    - "Comment aller de Paris à Lyon" → "Cmt aller de Paris à Lyon"
    """
    variations = []

    # Protection des entités
    protected_words = set()
    if entities.get("from"):
        protected_words.add(entities["from"].lower())
    if entities.get("to"):
        protected_words.add(entities["to"].lower())
    for via in entities.get("via", []):
        protected_words.add(via.lower())
    if entities.get("person"):
        protected_words.add(entities["person"].lower())

    # Décider quelle transformation appliquer
    rand = random.random()

    # 40% → Contractions orales
    if rand < probability * 0.4:
        variations.extend(_apply_contractions(text, entities, protected_words))

    # 40% → Abréviations texte
    elif rand < probability * 0.8:
        variations.extend(_apply_abbreviations(text, entities, protected_words))

    return variations


def _apply_contractions(text: str, entities: Dict, protected_words: set) -> List[tuple]:
    """Applique des contractions orales"""
    variations = []
    text_lower = text.lower()

    for pattern, replacements in ORAL_CONTRACTIONS.items():
        if pattern in text_lower:
            # Vérifier si le pattern touche une entité protégée
            if any(word in pattern for word in protected_words):
                continue

            # Choisir une contraction aléatoire
            replacement = random.choice(replacements)
            new_text = re.sub(
                rf'\b{re.escape(pattern)}\b',
                replacement,
                text,
                flags=re.IGNORECASE,
                count=1  # Une seule contraction par variation
            )

            if new_text != text:
                variations.append((new_text, entities.copy()))

    return variations


def _apply_abbreviations(text: str, entities: Dict, protected_words: set, max_abbrevs: int = 2) -> List[tuple]:
    """Applique des abréviations texte (max 2 par phrase)"""
    variations = []
    words = text.split()
    modified = False
    abbrev_count = 0

    for word_idx, word in enumerate(words):
        if abbrev_count >= max_abbrevs:
            break

        word_lower = word.lower().strip(".,!?")

        # Vérifier si le mot est protégé
        if word_lower in protected_words:
            continue

        # Vérifier si on a une abréviation pour ce mot
        if word_lower in TEXT_ABBREVIATIONS:
            if random.random() < 0.5:  # 50% de chance d'abréger ce mot spécifique
                abbrev = random.choice(TEXT_ABBREVIATIONS[word_lower])

                # Conserver la casse originale
                if word[0].isupper():
                    abbrev = abbrev.capitalize()

                words[word_idx] = word.replace(word_lower, abbrev, 1)
                modified = True
                abbrev_count += 1

    if modified:
        variations.append((" ".join(words), entities.copy()))

    return variations
