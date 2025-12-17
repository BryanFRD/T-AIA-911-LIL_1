"""
Augmentation : Fautes de frappe courantes
"""

import random
from typing import List, Dict

# Fautes de frappe courantes (élargi de 20 à 50+ règles)
TYPO_RULES = {
    # Verbes courants
    "aller": ["alé", "allé", "allez", "alér", "alléé", "allez", "alleer"],
    "partir": ["partire", "parrtir", "partirr", "patir"],
    "veux": ["veu", "veut", "veuxx", "vveux", "veus", "vx"],
    "voudrais": ["voudrè", "voudré", "voudrais", "vouudrai"],
    "prendre": ["prande", "pranre", "preendre", "prandre"],
    "cherche": ["cherch", "chérche", "cherchhe", "chearch"],
    "dois": ["doi", "doie", "doiss", "doit"],

    # Mots de liaison
    "de": ["dé", "de", "dè", "dd", "dde"],
    "à": ["a", "à", "aa", "aà", "ah"],
    "pour": ["por", "pur", "pouur", "pourr"],
    "avec": ["avéc", "aveck", "avvec", "avec"],
    "depuis": ["depui", "dépuis", "depuiss", "dpuis"],
    "vers": ["ver", "vèrs", "verss", "vrs"],
    "par": ["pa", "paar", "parr", "pr"],
    "en": ["enn", "én", "n"],
    "et": ["é", "ett", "èt", "ét"],

    # Interrogatifs
    "comment": ["comen", "comman", "comnt", "comennt", "coment", "coment"],
    "où": ["ou", "ouu", "oú", "òu"],
    "quand": ["quan", "quannt", "qand", "kand"],
    "pourquoi": ["pourkoi", "pourquoi", "pouurquoi", "prkoi"],
    "quel": ["kél", "quél", "qel", "quell"],

    # Noms communs transport
    "trajet": ["trajé", "trajais", "trajèt", "trajettt", "trajét", "trajjet"],
    "voyage": ["voyaje", "voiage", "voyagge", "voyyage"],
    "itinéraire": ["itineraire", "itinérère", "itinairre", "itinérraire"],
    "billet": ["billet", "bilet", "billettt", "bilé", "bilet"],
    "train": ["trn", "traiin", "trainn", "trin", "trèn"],
    "route": ["rout", "roote", "routte", "ruote"],
    "chemin": ["chemain", "chémin", "cheminn", "chmin"],

    # Pronoms/déterminants
    "je": ["jee", "jé", "jéé", "j", "jje"],
    "tu": ["ttu", "tú", "tue"],
    "il": ["ill", "íl", "iil"],
    "le": ["lé", "lee", "lè"],
    "la": ["là", "laa", "lâ"],
    "les": ["lés", "lès", "leés"],
    "un": ["unn", "únn", "1"],
    "une": ["unne", "úne", "uune"],

    # Adverbes/expressions
    "rapidement": ["rapidment", "rapidemment", "rapidemant", "rapidemnt"],
    "maintenant": ["maintenan", "méntenant", "maintnant", "maintnan"],
    "aujourd'hui": ["ojordui", "aujordhui", "aujourdui", "ojourdhui"],
    "demain": ["dmin", "démin", "demainn", "demein"],
    "tout": ["tou", "touu", "toutt", "tu"],
    "bien": ["bein", "bin", "bienn", "bién"],
    "merci": ["mérci", "mercii", "mrci", "mercy"],
    "bonjour": ["bonjourr", "bonjur", "bnjour", "bonjouur"],

    # Autres mots fréquents
    "besoin": ["bésoin", "besoinn", "bezoin", "bsoin"],
    "direct": ["dirèct", "diréct", "directt", "direc"],
    "rapide": ["rapîde", "rappide", "rapid", "rapde"],
    "prends": ["prend", "prendz", "prendsx", "pren"],
    "passe": ["pass", "passé", "passee", "pase"],
    "heure": ["eure", "heures", "heurre", "hr"],
}

def augment_typos(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Introduit des fautes de frappe courantes (sans toucher aux entités)

    Exemples:
    - "Je veu aller de Paris à Lyon"
    - "Je veux alé de Paris à Lyon"
    """
    variations = []

    # Ne pas modifier les noms de villes et prénoms
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

    for word_idx, word in enumerate(words):
        word_lower = word.lower().strip(".,!?")

        # Vérifier si le mot est protégé
        if word_lower in protected_words:
            continue

        # Appliquer les règles de typo
        if word_lower in TYPO_RULES:
            if random.random() < probability:
                typo_variant = random.choice(TYPO_RULES[word_lower])
                # Conserver la casse originale
                if word[0].isupper():
                    typo_variant = typo_variant.capitalize()
                words[word_idx] = word.replace(word_lower, typo_variant, 1)
                modified = True

    if modified:
        variations.append((" ".join(words), entities.copy()))

    return variations
