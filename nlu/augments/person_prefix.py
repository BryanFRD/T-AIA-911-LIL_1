"""
Augmentation: Ajout d'interpellations de personnes en préfixe

Ajoute des interpellations au début de la phrase:
- "Hey Maude, je veux aller à Paris"
- "Dis Simon, trajet Lyon Marseille"
- "Écoute Valérian, emmène-moi à Lyon"
"""

import random
from typing import List, Dict

# Préfixes d'interpellation enrichis
INTERPELLATIONS = [
    "Hey {person}",
    "Dis {person}",
    "Écoute {person}",
    "Ok {person}",
    "{person}",
    "Attends {person}",
    "Yo {person}",
    "Eh {person}",
    "Salut {person}",
    "Allo {person}",
    "Ouais {person}",
    "{person} écoute",
    "{person} bon",
    "Hé {person} alors",
    "Tu sais {person}",
    "Écoute-moi {person}",
    "Bon {person}",
    "Eh bien {person}",
]

# Personnes disponibles
PEOPLE = [
    "Maude",
    "Valérian",
    "Simon",
    "Jayce"
]

def augment_person_prefix(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Ajoute une interpellation de personne au début de la phrase

    Args:
        text: Le texte à augmenter
        entities: Les entités (from, to, via, person)
        probability: Probabilité d'appliquer cette augmentation

    Returns:
        Liste de (texte_augmenté, entités_augmentées)
    """
    if random.random() > probability:
        return []

    # Choisir une interpellation et une personne
    interpellation = random.choice(INTERPELLATIONS)
    person = random.choice(PEOPLE)

    # Construire le préfixe
    prefix = interpellation.format(person=person)

    # Nouveau texte avec virgule ou pas
    if random.random() < 0.7:  # 70% avec virgule
        augmented_text = f"{prefix}, {text}"
    else:  # 30% sans virgule
        augmented_text = f"{prefix} {text}"

    # Nouvelles entités (copie + ajout de person)
    new_entities = entities.copy()
    new_entities["person"] = person

    return [(augmented_text, new_entities)]
