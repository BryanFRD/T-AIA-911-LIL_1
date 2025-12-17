"""
Augmentation : Langage SMS/chat
"""

import random
from typing import List, Dict


# Transformations SMS/chat courantes
SMS_TRANSFORMATIONS = {
    "je": ["j", "jé"],
    "c'est": ["c", "c'é", "cé"],
    "quoi": ["koi", "kwa"],
    "comment": ["cmnt", "comen", "komen"],
    "pour": ["pr", "pr"],
    "veux": ["veu", "ve"],
    "aller": ["alé", "allé"],
    "tu": ["t"],
    "vous": ["vs"],
    "avec": ["avk", "ac"],
    "et": ["é", "&"],
    "de": ["d"],
    "à": ["a"],
    "oui": ["ui", "wi"],
    "train": ["tren", "trn"],
    "billet": ["biyet", "bilet"],
    "aussi": ["oci", "ossi"],
    "pas": ["pa"],
    "plus": ["plu", "+"],
    "demain": ["2m1", "dmain"],
    "aujourd'hui": ["ojourdui", "ojd"],
}


def augment_sms_language(text: str, entities: Dict) -> List[tuple]:
    """
    Transforme le texte en langage SMS/chat

    Exemples:
    - "Je veux aller" -> "J ve alé"
    - "Comment aller de Paris" -> "Komen alé de Paris"
    - "C'est quoi le trajet" -> "C koi le traj"
    """
    variations = []

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

    words = text.split()
    modified = False
    new_words = []

    for word in words:
        word_clean = word.lower().strip(".,!?")

        # Si c'est un mot protégé, ne pas le modifier
        if word_clean in protected_words:
            new_words.append(word)
        # Si on a une transformation SMS, l'appliquer avec une probabilité
        elif word_clean in SMS_TRANSFORMATIONS:
            if random.random() < 0.4:  # 40% de chance
                sms_version = random.choice(SMS_TRANSFORMATIONS[word_clean])
                # Conserver la ponctuation
                if word[-1] in ".,!?":
                    sms_version += word[-1]
                new_words.append(sms_version)
                modified = True
            else:
                new_words.append(word)
        else:
            new_words.append(word)

    if modified:
        variations.append((" ".join(new_words), entities.copy()))

    return variations
