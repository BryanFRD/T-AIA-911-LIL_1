"""
Augmentation : Entity Swap
Inverse FROM <-> TO pour créer des trajets retour
Utile pour générer diversité et tester robustesse du modèle
"""

import random
from typing import List, Dict

def augment_entity_swap(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Inverse les entités FROM et TO pour simuler des trajets retour

    Args:
        text: Texte à augmenter
        entities: Entités extraites (from, to, via, person)
        probability: Probabilité d'inverser FROM <-> TO

    Examples:
        - "Je veux aller de Paris à Lyon" -> "Je veux aller de Lyon à Paris"
        - "Trajet Marseille Bordeaux" -> "Trajet Bordeaux Marseille"
        - "De Lille vers Nantes" -> "De Nantes vers Lille"

    Returns:
        Liste de tuples (texte_augmenté, entités_swappées)

    Notes:
        - Ne swap que si FROM et TO existent tous les deux
        - Préserve VIA et PERSON (pas de swap)
        - Met à jour les entités pour refléter le swap
    """
    variations = []

    # Ne swap que si on a FROM et TO valides
    if not entities.get("from") or not entities.get("to"):
        return variations

    # Probabilité de swap
    if random.random() >= probability:
        return variations

    from_city = entities["from"]
    to_city = entities["to"]

    # Ne pas swap si identiques (cas rare mais possible)
    if from_city.lower() == to_city.lower():
        return variations

    # Remplacer dans le texte (attention à l'ordre pour éviter double remplacement)
    # Utiliser un placeholder temporaire
    placeholder = "<<PLACEHOLDER_CITY_SWAP>>"

    # Étape 1: Remplacer FROM par placeholder
    swapped_text = text.replace(from_city, placeholder)

    # Étape 2: Remplacer TO par FROM
    swapped_text = swapped_text.replace(to_city, from_city)

    # Étape 3: Remplacer placeholder par TO
    swapped_text = swapped_text.replace(placeholder, to_city)

    # Créer nouvelles entités avec FROM et TO inversés
    swapped_entities = entities.copy()
    swapped_entities["from"] = to_city
    swapped_entities["to"] = from_city
    # VIA et PERSON restent inchangés

    # Vérifier que le swap a bien eu lieu (le texte a changé)
    if swapped_text != text:
        variations.append((swapped_text, swapped_entities))

    return variations
