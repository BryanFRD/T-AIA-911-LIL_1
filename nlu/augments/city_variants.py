"""
Augmentation : Variantes de noms de villes
Génère des variantes de noms de villes (Saint->St, suppression tirets, abréviations)
"""

import json
import random
import re
from typing import List, Dict
from pathlib import Path

# Load abbreviation rules
_abbrev_config = None

def _load_abbrev_config():
    """Load city abbreviations config on first use."""
    global _abbrev_config
    if _abbrev_config is None:
        config_path = Path(__file__).parent.parent / 'data' / 'city_abbreviations.json'
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                _abbrev_config = json.load(f)
        except FileNotFoundError:
            _abbrev_config = {"rules": {}, "common_abbreviations": {}}
    return _abbrev_config


def _apply_city_variation(city_name: str, variation_type: str) -> str:
    """
    Apply a specific city name variation.
    
    Args:
        city_name: Original city name
        variation_type: Type of variation ('saint_to_st', 'remove_hyphens', 'abbreviation')
    
    Returns:
        Varied city name or original if variation doesn't apply
    """
    config = _load_abbrev_config()
    
    if variation_type == 'saint_to_st':
        # Saint-Etienne -> St-Etienne
        pattern = "^Saint-(.*)$"
        replacement = r"St-\1"
        if re.match(pattern, city_name):
            return re.sub(pattern, replacement, city_name)
    
    elif variation_type == 'remove_hyphens':
        # Aix-en-Provence -> Aix en Provence
        # Handle spaces when there's hyphenated City-word pattern
        if '-' in city_name:
            # First try common patterns like Aix-en-Provence
            pattern = r"^([A-Z][a-z]*)-([a-z].*)$"
            if re.match(pattern, city_name):
                return re.sub(pattern, r"\1 \2", city_name)
            # Then try ALL CAPS patterns
            pattern = r"^([A-Z][a-z]*)-([A-Z].*)$"
            if re.match(pattern, city_name):
                return re.sub(pattern, r"\1 \2", city_name)
    
    elif variation_type == 'abbreviation':
        # Get common abbreviations
        common_abbrev = config.get('common_abbreviations', {})
        return common_abbrev.get(city_name, city_name)
    
    return city_name


def _get_all_city_variations(city_name: str) -> List[str]:
    """
    Generate all possible variations for a city name.
    
    Returns:
        List of variation types and their results
    """
    variations = []
    
    for variation_type in ['saint_to_st', 'remove_hyphens', 'abbreviation']:
        varied = _apply_city_variation(city_name, variation_type)
        if varied != city_name:
            variations.append(varied)
    
    return variations


def _replace_city_in_text(text: str, original_city: str, variant_city: str) -> str:
    """
    Replace a city name in text, preserving case sensitivity where possible.
    
    Args:
        text: Full text
        original_city: Original city name
        variant_city: Variant city name
    
    Returns:
        Text with city replaced
    """
    # Simple word boundary replacement
    # Case-insensitive but preserve the replacement variant as-is
    pattern = r'\b' + re.escape(original_city) + r'\b'
    return re.sub(pattern, variant_city, text, flags=re.IGNORECASE)


def augment_city_variants(text: str, entities: Dict, probability: float) -> List[tuple]:
    """
    Generate city name variants (Saint->St, hyphens removal, abbreviations).
    
    Args:
        text: Texte à augmenter
        entities: Dict with 'from', 'to', 'via' (list) city names
        probability: Probabilité d'appliquer une variation à chaque ville
    
    Examples:
        - "Je veux aller de Saint-Etienne à Aix-en-Provence"
          -> "Je veux aller de St-Etienne à Aix en Provence"
        - "Trajet de Bordeaux à Paris"
          -> "Trajet de Bdx à Paris"
    
    Returns:
        Liste de tuples (texte_augmenté, entités)
    """
    variations = []
    
    # Collect all cities from entities
    cities_in_sample = []
    
    if entities.get("from"):
        cities_in_sample.append(("from", entities["from"]))
    if entities.get("to"):
        cities_in_sample.append(("to", entities["to"]))
    for via_city in entities.get("via", []):
        cities_in_sample.append(("via", via_city))
    
    if not cities_in_sample:
        return variations
    
    # Try different combinations of city variations
    # For each city, decide whether to apply a variation
    for attempt in range(3):  # Generate up to 3 variants per sample
        if random.random() > probability:
            continue
        
        new_text = text
        new_entities = entities.copy()
        if "via" in new_entities:
            new_entities["via"] = list(new_entities["via"])  # Make mutable copy
        
        modified = False
        
        for entity_type, city_name in cities_in_sample:
            if random.random() > probability:
                continue
            
            # Get all possible variations for this city
            city_variations = _get_all_city_variations(city_name)
            
            if city_variations:
                # Pick one random variation
                variant_city = random.choice(city_variations)
                
                # Replace in text
                new_text = _replace_city_in_text(new_text, city_name, variant_city)
                
                # Update entities
                if entity_type == "from":
                    new_entities["from"] = variant_city
                elif entity_type == "to":
                    new_entities["to"] = variant_city
                elif entity_type == "via":
                    # Find and replace in via list
                    for i, via in enumerate(new_entities.get("via", [])):
                        if via == city_name:
                            new_entities["via"][i] = variant_city
                
                modified = True
        
        if modified:
            variations.append((new_text, new_entities))
    
    return variations
