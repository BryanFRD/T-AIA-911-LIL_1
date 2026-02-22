"""
Context-aware augmentation filters for semantic understanding.
Helps prevent incorrect entity swap for beverage/food contexts.
"""

import re

# Verbs that indicate consumption/beverage context (base forms only - will match conjugations)
CONSUMPTION_VERBS = {
    "boir", "prend", "vers", "sirop", "mang", "goût", "dégust", "siroter",
    "serv", "apport", "préparer", "command", "réclaim", "demand", "donne"
}

# Words that indicate beverage/food items
BEVERAGE_KEYWORDS = {
    "monaco", "bière", "vin", "café", "thé", "menthe", 
    "orangina", "pastis", "verre", "gobelet",
    "boisson", "alcool", "apéro", "digestif", "jus", "smoothie",
    "cocktail", "punch", "champagne", "whisky", "vodka", "rhum",
    "genièvre", "tequila", "cidre", "eau", "limonade", "grenadine"
}

# Words that connect verbs to objects in consumption context
CONSUMPTION_CONNECTORS = {
    "un", "une", "de", "du", "de la", "des", "mon", "ma", "mes",
    "ton", "ta", "tes", "son", "sa", "ses", "leur", "nos", "votre"
}


def should_skip_entity_swap_for_beverage(text: str, entities: dict) -> bool:
    """
    Détecte si le texte contient un contexte de consommation de boisson/nourriture.
    Si oui, recommande de ne pas faire d'entity_swap (qui renforcerait l'interprétation location).
    
    Args:
        text: The text to analyze
        entities: Entity dictionary from template generation
        
    Returns:
        True if entity_swap should be SKIPPED (beverage context detected)
        False if entity_swap is OK
    """
    text_lower = text.lower()
    
    # Check if consumption verbs are present (matching verb stems)
    has_consumption_verb = False
    for verb_stem in CONSUMPTION_VERBS:
        # Match words containing the verb stem (e.g., "boir" matches "boire", "bois", "boit", "buvons", "buvait", etc.)
        if re.search(r'\b' + verb_stem + r'\w*', text_lower):
            has_consumption_verb = True
            break
    
    if not has_consumption_verb:
        return False
    
    # Check if beverage keywords are present
    has_beverage_keyword = any(keyword in text_lower for keyword in BEVERAGE_KEYWORDS)
    
    if has_beverage_keyword:
        return True
    
    # Additional heuristic: if text contains certain patterns
    # "Un/Une/De <word>" where <word> could be a beverage
    article_pattern = r'(un|une|de|du|de la|des)\s+(\w+)'
    matches = re.findall(article_pattern, text_lower)
    
    for article, word in matches:
        if word in BEVERAGE_KEYWORDS:
            return True
    
    return False


def get_consumption_context_score(text: str) -> float:
    """
    Returns a confidence score (0-1) indicating how likely the text 
    is about beverage/food consumption rather than location-based travel.
    
    Args:
        text: The text to analyze
        
    Returns:
        Float between 0 and 1 indicating consumption context probability
    """
    text_lower = text.lower()
    score = 0.0
    
    # Count consumption verbs (matching stems)
    verb_count = 0
    for verb_stem in CONSUMPTION_VERBS:
        if re.search(r'\b' + verb_stem + r'\w*', text_lower):
            verb_count += 1
    score += min(verb_count * 0.3, 0.5)  # Max 0.5 from verbs
    
    # Count beverage keywords
    beverage_count = sum(1 for keyword in BEVERAGE_KEYWORDS if keyword in text_lower)
    score += min(beverage_count * 0.25, 0.5)  # Max 0.5 from beverages
    
    return min(score, 1.0)


def filter_ner_locations_in_beverage_context(text: str, predicted_entities: list) -> list:
    """
    Post-processing filter: Reduce confidence of location entity tags
    when strong beverage consumption context is detected.
    
    Args:
        text: Source text
        predicted_entities: List of (entity_type, token, confidence) tuples
        
    Returns:
        Modified entity list with adjusted confidence scores
    """
    context_score = get_consumption_context_score(text)
    
    if context_score < 0.3:
        return predicted_entities
    
    # Reduce location entity confidence by context score
    filtered = []
    for entity in predicted_entities:
        entity_type = entity[0] if isinstance(entity, tuple) else entity.get("entity_type")
        
        if entity_type in ["B-TO", "I-TO", "B-FROM", "I-FROM"]:
            # Reduce confidence scores for location entities
            if isinstance(entity, tuple):
                entity_type, token, confidence = entity
                new_confidence = confidence * (1 - context_score * 0.5)
                filtered.append((entity_type, token, new_confidence))
            else:
                entity["confidence"] = entity.get("confidence", 1.0) * (1 - context_score * 0.5)
                filtered.append(entity)
        else:
            filtered.append(entity)
    
    return filtered
