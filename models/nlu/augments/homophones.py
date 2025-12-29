"""
Augmentation : Homophones
Remplace aléatoirement des mots par leurs homophones (erreurs courantes)
"""

import random
from typing import List, Dict

# Mapping des homophones courants en français
# Format: mot_correct -> [homophones_incorrects]
HOMOPHONES_MAP = {
    # Homophones grammaticaux très fréquents
    "à": ["a"],
    "a": ["à"],
    "où": ["ou"],
    "ou": ["où"],
    "ce": ["se"],
    "se": ["ce"],
    "et": ["est", "ai"],
    "est": ["et", "ai"],
    "son": ["sont"],
    "sont": ["son"],
    "ces": ["ses", "c'est", "sais", "sait"],
    "ses": ["ces", "c'est", "sais", "sait"],

    # Homophones avec apostrophe
    "c'est": ["s'est", "sais", "sait", "ces", "ses"],
    "s'est": ["c'est", "sais", "sait"],
    "sais": ["sait", "c'est", "s'est"],
    "sait": ["sais", "c'est", "s'est"],

    # Autres homophones courants
    "ça": ["sa"],
    "sa": ["ça"],
    "la": ["là", "l'a"],
    "là": ["la", "l'a"],
    "on": ["ont"],
    "ont": ["on"],
    "mes": ["mais", "met", "mets"],
    "mais": ["mes", "met", "mets"],
    "peu": ["peut", "peux"],
    "peut": ["peu", "peux"],
    "peux": ["peu", "peut"],
    "leur": ["leurs", "l'heure"],
    "leurs": ["leur", "l'heure"],
    "tous": ["tout", "toux"],
    "tout": ["tous", "toux"],
    "voir": ["voire"],
    "voire": ["voir"],
    "si": ["s'y"],
    "s'y": ["si"],
    "ni": ["n'y"],
    "n'y": ["ni"],
    "sans": ["sang", "sens", "sent"],
    "sang": ["sans", "sens", "sent"],
    "temps": ["tant", "taon"],
    "tant": ["temps", "taon"],
    "dans": ["dent", "d'en"],
    "dent": ["dans", "d'en"],
    "quand": ["quant", "qu'en"],
    "quant": ["quand", "qu'en"],
    "quel": ["quelle", "qu'elle"],
    "quelle": ["quel", "qu'elle"],
    "qu'elle": ["quel", "quelle"],

    # Homophones transport/voyage
    "voie": ["voix", "vois", "voit"],
    "voix": ["voie", "vois", "voit"],
    "vois": ["voie", "voix", "voit"],
    "voit": ["voie", "voix", "vois"],
    "cour": ["cours", "court"],
    "cours": ["cour", "court"],
    "court": ["cour", "cours"],
    "part": ["par", "pars"],
    "par": ["part", "pars"],
    "pars": ["part", "par"],
}

def augment_homophones(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Remplace aléatoirement des mots par leurs homophones (erreurs grammaticales courantes)

    Args:
        text: Texte à augmenter
        entities: Entités à préserver (ne pas modifier noms de villes/personnes)
        probability: Probabilité de remplacer un mot par son homophone

    Examples:
        - "Je veux aller à Paris" -> "Je veux aller a Paris"
        - "Où est la gare ?" -> "Ou est la gare ?"
        - "C'est un trajet rapide" -> "Sait un trajet rapide"

    Returns:
        Liste de tuples (texte_augmenté, entités)
    """
    variations = []

    # Ne pas modifier les noms de villes et prénoms (protéger entités)
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

    words = text.split()
    modified = False
    result_words = []

    for word in words:
        # Extraire la ponctuation
        punctuation = ""
        clean_word = word
        if word and word[-1] in ".,!?;:":
            punctuation = word[-1]
            clean_word = word[:-1]

        word_lower = clean_word.lower()

        # Si le mot est protégé, on ne le modifie pas
        if word_lower in protected_words:
            result_words.append(word)
            continue

        # Appliquer homophone si disponible
        if word_lower in HOMOPHONES_MAP:
            if random.random() < probability:
                homophone = random.choice(HOMOPHONES_MAP[word_lower])

                # Conserver la casse originale (majuscule initiale)
                if clean_word and clean_word[0].isupper():
                    homophone = homophone.capitalize()

                result_words.append(homophone + punctuation)
                modified = True
            else:
                result_words.append(word)
        else:
            result_words.append(word)

    if modified:
        variations.append((" ".join(result_words), entities.copy()))

    return variations
