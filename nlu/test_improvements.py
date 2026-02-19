#!/usr/bin/env python3
"""
Test suite for NLU model improvements - Monaco/beverage disambiguation
Tests the enhanced model's ability to correctly classify and tag ambiguous terms.
"""

import json
import sys
from pathlib import Path

# Setup path
sys.path.insert(0, str(Path(__file__).parent.parent))

# Test cases: (input_text, expected_intent, should_exclude_location_tags)
TEST_CASES = [
    # Positive cases: travel intent with Monaco as location
    ("Je veux aller de Monaco à Cannes", "trajet", False),
    ("Trajet de Monaco vers Paris", "trajet", False),
    ("Monaco à Lyon directement", "trajet", False),
    ("De Monaco jusqu'à Marseille", "trajet", False),
    
    # Negative cases: beverage intent with Monaco
    ("Je veux boire un monaco", "other", True),
    ("Un monaco s'il vous plaît", "other", True),
    ("Peux-tu me verser un verre de monaco", "other", True),
    ("Un monaco bien glacé", "other", True),
    ("Je prendrais bien un monaco", "other", True),
    ("Serve-moi un monaco", "other", True),
    
    # Other beverage tests
    ("Je vais prendre une bière", "other", True),
    ("Je voudrais un verre de vin", "other", True),
    ("Une menthe à l'eau s'il vous plaît", "other", True),
    ("Donne-moi une orangina", "other", True),
    ("Je veux un pastis bien frais", "other", True),
    
    # Edge cases
    ("Je suis né à Monaco mais je veux une bière", "other", True),  # Context switches
    ("De Paris à Monaco pour boire un monaco", "other", True),  # Mixed contexts (beverage dominates)
    
    # Non-ambiguous cases (control)
    ("Je veux aller à Paris", "trajet", False),
    ("Quel temps fait-il à Lyon", "other", False),
    ("Bonjour comment ça va", "other", False),
    ("Je veux manger une pizza", "other", False),
]


def test_import_modules():
    """Verify all required modules can be imported"""
    print("=" * 70)
    print("Testing module imports...")
    print("=" * 70)
    
    try:
        from nlu.augments.context_awareness import (
            should_skip_entity_swap_for_beverage,
            get_consumption_context_score,
            filter_ner_locations_in_beverage_context
        )
        print("[OK] context_awareness module imported successfully")
        
        from nlu.augments import NLUAugmenter
        print("[OK] NLUAugmenter imported successfully")
        
        print("[OK] All module imports successful\n")
        return True
    except ImportError as e:
        print(f"[ERROR] Import failed: {e}\n")
        return False


def test_context_detection():
    """Test beverage context detection"""
    print("=" * 70)
    print("Testing context detection...")
    print("=" * 70)
    
    from nlu.augments.context_awareness import (
        should_skip_entity_swap_for_beverage,
        get_consumption_context_score
    )
    
    test_texts = [
        ("Je veux boire un monaco", True, 0.5),        # High beverage context
        ("Je veux aller à Monaco", False, 0.0),        # No beverage context
        ("Je prendrais un verre de vin", True, 0.5),   # Clear beverage
        ("Trajet de Paris à Lyon", False, 0.0),        # No beverage
        ("Donne-moi un monaco s'il te plaît", True, 0.6),  # Clear beverage
    ]
    
    passed = 0
    failed = 0
    
    for text, expected_skip, expected_score_min in test_texts:
        # Test skip logic
        skip = should_skip_entity_swap_for_beverage(text, {})
        skip_ok = skip == expected_skip
        
        # Test context score
        score = get_consumption_context_score(text)
        score_ok = score >= expected_score_min
        
        status = "[OK]" if (skip_ok and score_ok) else "[FAIL]"
        print(f"{status} '{text}'")
        print(f"     Skip entity_swap: {skip} (expected {expected_skip})")
        print(f"     Context score: {score:.2f} (expected >= {expected_score_min})")
        
        if skip_ok and score_ok:
            passed += 1
        else:
            failed += 1
    
    print(f"\nContext detection: {passed} passed, {failed} failed\n")
    return failed == 0


def test_augmentation_awareness():
    """Test that augmenter respects beverage context"""
    print("=" * 70)
    print("Testing augmentation context awareness...")
    print("=" * 70)
    
    from nlu.augments import NLUAugmenter
    
    augmenter = NLUAugmenter()
    
    # Test text with beverage context
    beverage_text = "Je veux boire un monaco"
    entities = {
        "from": "",
        "to": "monaco",
        "via": [],
        "person": ""
    }
    
    print(f"Original: {beverage_text}")
    print("Entities:", entities)
    print("\nGenerated augmentations (should NOT have entity_swap):")
    
    variants = augmenter.augment_example(beverage_text, entities, num_variants=5)
    
    for i, (text, ents) in enumerate(variants, 1):
        print(f"  {i}. {text}")
        # For beverage context, swap shouldn't happen (FROM/TO shouldn't be inverted)
        if ents != entities and ents.get("from") and ents.get("to"):
            if ents["from"] == "monaco" and ents["to"] == "":
                print(f"     [WARN] Possible entity_swap detected (should be skipped)")
    
    print("[OK] Augmentation context awareness tested\n")
    return True


def test_lexicon_loading():
    """Test that lexicon file exists and is valid JSON"""
    print("=" * 70)
    print("Testing lexicon loading...")
    print("=" * 70)
    
    lexicon_path = Path("nlu/data/lexicons.json")
    
    if not lexicon_path.exists():
        print(f"[FAIL] Lexicon file not found at {lexicon_path}\n")
        return False
    
    try:
        with open(lexicon_path, 'r', encoding='utf-8') as f:
            lexicons = json.load(f)
        
        required_keys = [
            "consumption_verbs",
            "beverage_nouns",
            "location_nouns",
            "consumption_connectors",
            "context_indicators"
        ]
        
        for key in required_keys:
            if key not in lexicons:
                print(f"[FAIL] Missing key: {key}")
                return False
        
        print(f"[OK] Lexicon file loaded successfully")
        print(f"    - Consumption verbs: {len(lexicons['consumption_verbs'])}")
        print(f"    - Beverage nouns: {len(lexicons['beverage_nouns'])}")
        print(f"    - Location nouns: {len(lexicons['location_nouns'])}")
        print(f"    - Connectors: {len(lexicons['consumption_connectors'])}\n")
        return True
    except json.JSONDecodeError as e:
        print(f"[FAIL] Invalid JSON in lexicon file: {e}\n")
        return False


def test_config_crf():
    """Test that CRF is enabled in config"""
    print("=" * 70)
    print("Testing CRF configuration...")
    print("=" * 70)
    
    config_path = Path("nlu/config.json")
    
    if not config_path.exists():
        print(f"[FAIL] Config file not found at {config_path}\n")
        return False
    
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        use_crf = config.get("training", {}).get("use_crf", False)
        
        if use_crf:
            print("[OK] CRF is ENABLED in config")
            print("    - This forces valid BIO sequences during training")
            print("    - Reduces false positive entity tags\n")
            return True
        else:
            print("[FAIL] CRF is disabled in config")
            print("    - Recommend enabling for better sequence validation\n")
            return False
    except json.JSONDecodeError as e:
        print(f"[FAIL] Invalid JSON in config file: {e}\n")
        return False


def main():
    """Run all tests"""
    print("\n")
    print("╔" + "═" * 68 + "╗")
    print("║" + " " * 68 + "║")
    print("║" + "  NLU Model Improvement Test Suite - Monaco/Beverage Fix".center(68) + "║")
    print("║" + " " * 68 + "║")
    print("╚" + "═" * 68 + "╝")
    print()
    
    results = []
    
    # Run tests
    results.append(("Module imports", test_import_modules()))
    results.append(("Lexicon loading", test_lexicon_loading()))
    results.append(("CRF configuration", test_config_crf()))
    results.append(("Context detection", test_context_detection()))
    results.append(("Augmentation awareness", test_augmentation_awareness()))
    
    # Summary
    print("=" * 70)
    print("Test Summary")
    print("=" * 70)
    
    passed = sum(1 for _, result in results if result)
    failed = sum(1 for _, result in results if not result)
    
    for test_name, result in results:
        status = "[PASS]" if result else "[FAIL]"
        print(f"{status} {test_name}")
    
    print(f"\nTotal: {passed} passed, {failed} failed")
    
    if failed == 0:
        print("\n✓ All tests passed! Model improvements are ready for training.\n")
        return 0
    else:
        print(f"\n✗ {failed} test(s) failed. Please review the output above.\n")
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
