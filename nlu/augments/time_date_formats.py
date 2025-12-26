"""
Augmentation: Variations de formats de temps et dates
Transforme les heures et dates en différents formats pour diversifier le dataset
"""

import random
import re
from typing import List, Dict, Tuple


# Mapping des transformations de temps
TIME_VARIATIONS = {
    # Format standard -> variations
    r'\b8h\b': ['8 heures', 'huit heures', '8:00', '8h00'],
    r'\b8h30\b': ['8 heures 30', 'huit heures trente', '8:30', '8h30', 'huit heures et demie'],
    r'\b9h\b': ['9 heures', 'neuf heures', '9:00', '9h00'],
    r'\b9h15\b': ['9 heures 15', 'neuf heures quinze', '9:15', '9h15', 'neuf heures et quart'],
    r'\b9h30\b': ['9 heures 30', 'neuf heures trente', '9:30', '9h30', 'neuf heures et demie'],
    r'\b10h\b': ['10 heures', 'dix heures', '10:00', '10h00'],
    r'\b10h30\b': ['10 heures 30', 'dix heures trente', '10:30', '10h30', 'dix heures et demie'],
    r'\b11h\b': ['11 heures', 'onze heures', '11:00', '11h00'],
    r'\b11h30\b': ['11 heures 30', 'onze heures trente', '11:30', '11h30', 'onze heures et demie'],
    r'\b12h\b': ['12 heures', 'douze heures', '12:00', '12h00', 'midi'],
    r'\b12h30\b': ['12 heures 30', 'douze heures trente', '12:30', '12h30'],
    r'\b13h\b': ['13 heures', 'treize heures', '13:00', '13h00', '1h de l\'après-midi'],
    r'\b13h30\b': ['13 heures 30', 'treize heures trente', '13:30', '13h30', '1h30 de l\'après-midi'],
    r'\b14h\b': ['14 heures', 'quatorze heures', '14:00', '14h00', '2h de l\'après-midi'],
    r'\b14h15\b': ['14 heures 15', 'quatorze heures quinze', '14:15', '14h15', '2h15 de l\'après-midi'],
    r'\b14h30\b': ['14 heures 30', 'quatorze heures trente', '14:30', '14h30', '2h30 de l\'après-midi'],
    r'\b15h\b': ['15 heures', 'quinze heures', '15:00', '15h00', '3h de l\'après-midi'],
    r'\b15h30\b': ['15 heures 30', 'quinze heures trente', '15:30', '15h30', '3h30 de l\'après-midi'],
    r'\b16h\b': ['16 heures', 'seize heures', '16:00', '16h00', '4h de l\'après-midi'],
    r'\b16h30\b': ['16 heures 30', 'seize heures trente', '16:30', '16h30', '4h30 de l\'après-midi'],
    r'\b17h\b': ['17 heures', 'dix-sept heures', '17:00', '17h00', '5h de l\'après-midi'],
    r'\b17h30\b': ['17 heures 30', 'dix-sept heures trente', '17:30', '17h30', '5h30 de l\'après-midi'],
    r'\b18h\b': ['18 heures', 'dix-huit heures', '18:00', '18h00', '6h du soir'],
    r'\b18h30\b': ['18 heures 30', 'dix-huit heures trente', '18:30', '18h30', '6h30 du soir'],
    r'\b19h\b': ['19 heures', 'dix-neuf heures', '19:00', '19h00', '7h du soir'],
    r'\b19h30\b': ['19 heures 30', 'dix-neuf heures trente', '19:30', '19h30', '7h30 du soir'],
    r'\b20h\b': ['20 heures', 'vingt heures', '20:00', '20h00', '8h du soir'],
    r'\b20h30\b': ['20 heures 30', 'vingt heures trente', '20:30', '20h30', '8h30 du soir'],
    r'\b21h\b': ['21 heures', 'vingt-et-une heures', '21:00', '21h00', '9h du soir'],
    r'\b22h\b': ['22 heures', 'vingt-deux heures', '22:00', '22h00', '10h du soir'],
    r'\b23h\b': ['23 heures', 'vingt-trois heures', '23:00', '23h00', '11h du soir'],
    r'\bdans 1h\b': ['dans une heure', 'd\'ici une heure', 'd\'ici 1h', 'dans 1 heure'],
    r'\bdans 2h\b': ['dans deux heures', 'd\'ici deux heures', 'd\'ici 2h', 'dans 2 heures'],
    r'\bdans 3h\b': ['dans trois heures', 'd\'ici trois heures', 'd\'ici 3h', 'dans 3 heures'],
    r'\bdans 30 minutes\b': ['dans une demi-heure', 'd\'ici 30 minutes', 'dans trente minutes'],
    r'\bdans une heure\b': ['dans 1h', 'd\'ici une heure', 'd\'ici 1h'],
}

# Mapping des transformations de dates
DATE_VARIATIONS = {
    r'\bdemain\b': ['demain', 'le lendemain', 'demain matin'],
    r'\baprès-demain\b': ['après-demain', 'après demain', 'le surlendemain'],
    r'\bce soir\b': ['ce soir', 'en soirée', 'dans la soirée'],
    r'\bce matin\b': ['ce matin', 'dans la matinée', 'en matinée'],
    r'\bcet après-midi\b': ['cet après-midi', 'cet aprem', 'dans l\'après-midi'],
    r'\baujourd\'hui\b': ['aujourd\'hui', 'aujourd hui', 'dans la journée'],
    r'\bce week-end\b': ['ce week-end', 'ce weekend', 'en fin de semaine'],
    r'\bla semaine prochaine\b': ['la semaine prochaine', 'semaine prochaine'],
    r'\ble mois prochain\b': ['le mois prochain', 'mois prochain'],
    r'\blundi\b': ['lundi', 'ce lundi'],
    r'\bmardi\b': ['mardi', 'ce mardi'],
    r'\bmercredi\b': ['mercredi', 'ce mercredi'],
    r'\bjeudi\b': ['jeudi', 'ce jeudi'],
    r'\bvendredi\b': ['vendredi', 'ce vendredi'],
    r'\bsamedi\b': ['samedi', 'ce samedi'],
    r'\bdimanche\b': ['dimanche', 'ce dimanche'],
    r'\blundi prochain\b': ['lundi prochain', 'le lundi suivant'],
    r'\bmardi prochain\b': ['mardi prochain', 'le mardi suivant'],
    r'\bmercredi prochain\b': ['mercredi prochain', 'le mercredi suivant'],
    r'\bjeudi prochain\b': ['jeudi prochain', 'le jeudi suivant'],
    r'\bvendredi prochain\b': ['vendredi prochain', 'le vendredi suivant'],
}


def augment_time_date_formats(text: str, entities: Dict, probability: float = 0.5) -> List[Tuple[str, Dict]]:
    """
    Applique des variations de formats de temps et dates

    Args:
        text: Texte original
        entities: Entités extraites
        probability: Probabilité d'appliquer cette transformation

    Returns:
        Liste de (texte_augmenté, entités)
    """
    if random.random() > probability:
        return []

    # Check if text contains time or date entities
    has_time = entities.get("time", "")
    has_date = entities.get("date", "")

    if not has_time and not has_date:
        return []

    variants = []

    # Apply time variations
    if has_time:
        for pattern, replacements in TIME_VARIATIONS.items():
            if re.search(pattern, text, re.IGNORECASE):
                # Try 1-2 random variations
                for _ in range(random.randint(1, 2)):
                    replacement = random.choice(replacements)
                    new_text = re.sub(pattern, replacement, text, count=1, flags=re.IGNORECASE)

                    # Update time entity to match new format
                    new_entities = entities.copy()
                    new_entities["time"] = replacement

                    if new_text != text:
                        variants.append((new_text, new_entities))

    # Apply date variations
    if has_date:
        for pattern, replacements in DATE_VARIATIONS.items():
            if re.search(pattern, text, re.IGNORECASE):
                # Try 1-2 random variations
                for _ in range(random.randint(1, 2)):
                    replacement = random.choice(replacements)
                    new_text = re.sub(pattern, replacement, text, count=1, flags=re.IGNORECASE)

                    # Update date entity to match new format
                    new_entities = entities.copy()
                    new_entities["date"] = replacement

                    if new_text != text:
                        variants.append((new_text, new_entities))

    return variants[:3]  # Limit to 3 variants per example


if __name__ == "__main__":
    # Test
    test_examples = [
        ("Je veux un train demain à 14h30", {"time": "14h30", "date": "demain", "from": "Paris", "to": "Lyon"}),
        ("Train dans 2h pour Lyon", {"time": "dans 2h", "date": "", "from": "", "to": "Lyon"}),
        ("Départ ce matin à 9h15", {"time": "9h15", "date": "ce matin", "from": "Paris", "to": "Lyon"}),
    ]

    for text, entities in test_examples:
        print(f"\nOriginal: {text}")
        variants = augment_time_date_formats(text, entities, probability=1.0)
        for variant_text, variant_entities in variants:
            print(f"  → {variant_text}")
            if variant_entities.get("time") != entities.get("time"):
                print(f"    TIME: {entities.get('time')} → {variant_entities.get('time')}")
            if variant_entities.get("date") != entities.get("date"):
                print(f"    DATE: {entities.get('date')} → {variant_entities.get('date')}")
