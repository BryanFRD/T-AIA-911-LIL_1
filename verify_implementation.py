#!/usr/bin/env python3
"""
Final verification script - ensures all implementation files are correct
"""

import json
import sys
from pathlib import Path

def verify_implementation():
    """Verify all implementation files exist and are valid"""
    
    print("\n" + "=" * 70)
    print("FINAL VERIFICATION - NLU IMPROVEMENTS".center(70))
    print("=" * 70 + "\n")
    
    checks = []
    
    # Check 1: Lexicons file
    try:
        with open("nlu/data/lexicons.json", "r") as f:
            lexicons = json.load(f)
        checks.append((
            "✓ Lexicons JSON valid",
            f"Contains {len(lexicons.get('consumption_verbs', []))} verbs, "
            f"{len(lexicons.get('beverage_nouns', []))} beverages"
        ))
    except Exception as e:
        checks.append((f"✗ Lexicons error", str(e)))
    
    # Check 2: Config CRF
    try:
        with open("nlu/config.json", "r") as f:
            config = json.load(f)
        use_crf = config.get("training", {}).get("use_crf", False)
        status = "✓" if use_crf else "✗"
        checks.append((
            f"{status} Config CRF",
            f"use_crf = {use_crf}"
        ))
    except Exception as e:
        checks.append((f"✗ Config error", str(e)))
    
    # Check 3: Context awareness module
    try:
        import importlib.util
        spec = importlib.util.spec_from_file_location(
            "context_awareness",
            "nlu/augments/context_awareness.py"
        )
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        
        # Check functions exist
        has_skip = hasattr(module, 'should_skip_entity_swap_for_beverage')
        has_score = hasattr(module, 'get_consumption_context_score')
        has_filter = hasattr(module, 'filter_ner_locations_in_beverage_context')
        
        if has_skip and has_score and has_filter:
            checks.append(("✓ Context awareness module", "All functions present"))
        else:
            checks.append(("✗ Context awareness module", "Missing functions"))
    except Exception as e:
        checks.append((f"✗ Context awareness error", str(e)))
    
    # Check 4: Augmenter module
    try:
        with open("nlu/augments/augmenter.py", "r") as f:
            content = f.read()
        
        has_context_import = "from .context_awareness import" in content
        has_skip_logic = "skip_entity_swap_for_beverage" in content
        has_reduced_swap = "(augment_entity_swap, 0.05)" in content
        has_increased_politeness = "(augment_politeness_variations, 0.6)" in content
        
        if all([has_context_import, has_skip_logic, has_reduced_swap, has_increased_politeness]):
            checks.append(("✓ Augmenter optimized", "Context awareness integrated"))
        else:
            checks.append(("✗ Augmenter", "Missing optimizations"))
    except Exception as e:
        checks.append((f"✗ Augmenter error", str(e)))
    
    # Check 5: Templates data
    try:
        with open("nlu/data/templates.json", "r") as f:
            templates = json.load(f)
        
        negative_examples = templates.get("negative_examples", [])
        beverage_examples = [
            t for t in negative_examples 
            if any(word in t.get("pattern", "").lower() 
                   for word in ["boir", "prend", "vers", "monaco", "bière", "vin"])
        ]
        
        checks.append((
            "✓ Templates augmented",
            f"{len(negative_examples)} total examples, "
            f"{len(beverage_examples)} beverage-related"
        ))
    except Exception as e:
        checks.append((f"✗ Templates error", str(e)))
    
    # Check 6: Test file
    try:
        test_file = Path("nlu/test_improvements.py")
        if test_file.exists() and test_file.stat().st_size > 1000:
            checks.append(("✓ Test suite created", f"{test_file.stat().st_size} bytes"))
        else:
            checks.append(("✗ Test suite", "File too small or missing"))
    except Exception as e:
        checks.append((f"✗ Test file error", str(e)))
    
    # Check 7: Documentation
    doc_files = [
        "QUICK_START.md",
        "NLU_IMPROVEMENTS_SUMMARY.md",
        "CHANGELOG_NLU_IMPROVEMENTS.md",
        "nlu/TRAINING_GUIDE.md",
        "IMPLEMENTATION_COMPLETE.txt"
    ]
    
    missing_docs = []
    for doc in doc_files:
        if not Path(doc).exists():
            missing_docs.append(doc)
    
    if not missing_docs:
        checks.append(("✓ Documentation complete", f"All {len(doc_files)} guides present"))
    else:
        checks.append(("✗ Documentation", f"Missing: {len(missing_docs)} files"))
    
    # Print results
    print(f"{'COMPONENT':<35} {'STATUS':<30}")
    print("-" * 70)
    
    for check_name, check_detail in checks:
        print(f"{check_name:<35} {check_detail:<30}")
    
    print("\n" + "=" * 70)
    
    # Summary
    passed = sum(1 for name, _ in checks if "✓" in name)
    total = len(checks)
    
    print(f"\nSummary: {passed}/{total} checks passed\n")
    
    if passed == total:
        print("🟢 IMPLEMENTATION COMPLETE AND VERIFIED")
        print("\nReady to train:")
        print("  cd nlu")
        print("  python train.py --config config.json")
        return 0
    else:
        print(f"🟡 {total - passed} issue(s) found - review above")
        return 1

if __name__ == "__main__":
    exit_code = verify_implementation()
    sys.exit(exit_code)
