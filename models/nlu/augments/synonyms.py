"""
Augmentation : Synonymes contextuels
"""

import random
from typing import List, Dict


# Synonymes pour variété (élargi de 4 à 50+ entrées)
SYNONYMS = {
    # Verbes de déplacement
    "aller": ["me rendre", "voyager", "partir", "me déplacer", "rejoindre", "filer", "rouler"],
    "partir": ["décamper", "me casser", "me barrer", "décoller", "me tirer"],
    "rejoindre": ["retrouver", "atteindre", "gagner", "rallier"],
    "arriver": ["parvenir", "atteindre", "aboutir", "débarquer"],
    "prendre": ["emprunter", "utiliser", "choisir", "opter pour"],

    # Verbes de volonté/désir
    "veux": ["voudrais", "souhaite", "aimerais", "désire", "compte", "envisage"],
    "voudrais": ["souhaiterais", "aimerais bien", "désirerais"],
    "dois": ["faut que j'aille", "il faut que", "suis obligé de"],
    "cherche": ["recherche", "essaie de trouver", "tente de trouver"],

    # Noms de trajet
    "trajet": ["voyage", "itinéraire", "route", "parcours", "chemin", "déplacement"],
    "itinéraire": ["parcours", "route", "chemin", "trajet"],
    "route": ["voie", "chemin", "trajet", "itinéraire"],
    "voyage": ["déplacement", "trajet", "expédition"],
    "chemin": ["voie", "route", "passage", "itinéraire"],

    # Prépositions/locutions
    "depuis": ["à partir de", "en partant de", "de", "au départ de"],
    "vers": ["en direction de", "pour", "jusqu'à", "destination"],
    "à": ["vers", "pour", "jusqu'à", "en direction de"],
    "pour": ["vers", "à destination de", "en direction de"],
    "via": ["en passant par", "par", "avec escale à"],
    "par": ["via", "en passant par"],

    # Qualificatifs de vitesse/qualité
    "rapide": ["vite", "rapidement", "express", "direct", "speed"],
    "direct": ["sans arrêt", "sans escale", "en direct", "non-stop"],
    "court": ["bref", "proche", "rapproché"],
    "long": ["lointain", "éloigné", "distant"],
    "meilleur": ["optimal", "top", "idéal"],

    # Interrogatifs
    "comment": ["comment faire pour", "de quelle manière", "par quel moyen"],
    "où": ["à quel endroit", "dans quelle ville"],
    "quand": ["à quel moment", "à quelle heure"],
    "pourquoi": ["pour quelle raison", "dans quel but"],

    # Expressions courantes
    "il faut": ["je dois", "il est nécessaire de", "faut que"],
    "je peux": ["j'arrive à", "je suis capable de", "c'est possible de"],
    "y'a": ["il y a", "existe"],
    "besoin": ["nécessité", "envie", "obligation"],

    # Moyens de transport
    "train": ["TGV", "rail", "locomotive"],
    "bus": ["car", "autobus", "autocar"],
    "voiture": ["bagnole", "caisse", "auto", "véhicule"],
    "avion": ["vol", "aérien"],

    # Termes temporels
    "maintenant": ["là", "tout de suite", "immédiatement", "actuellement"],
    "aujourd'hui": ["ce jour", "à présent"],
    "demain": ["le lendemain", "jour suivant"],
    "bientôt": ["prochainement", "sous peu", "rapidement"],

    # Verbes d'information
    "donner": ["fournir", "indiquer", "communiquer"],
    "montrer": ["indiquer", "afficher", "présenter"],
    "chercher": ["rechercher", "trouver", "dénicher"],
    "trouver": ["dénicher", "localiser", "repérer"],

    # Autres
    "bon": ["ok", "bien", "d'accord", "ouais"],
    "s'il vous plaît": ["svp", "s'il te plaît", "merci"],
    "merci": ["thanks", "cool", "super"],
}


def augment_synonyms(text: str, entities: Dict, probability: float) -> List[tuple]:
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
            if random.random() < probability:
                synonym = random.choice(SYNONYMS[word_lower])
                # Conserver la casse
                if word[0].isupper():
                    synonym = synonym.capitalize()

                new_words = words.copy()
                new_words[word_idx] = word.replace(word_lower, synonym, 1)
                variations.append((" ".join(new_words), entities.copy()))

    return variations
