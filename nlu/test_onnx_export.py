"""
Script de test pour l'export ONNX des modèles NLU

Usage:
    python nlu/test_onnx_export.py
"""

import torch
import numpy as np
from transformers import AutoTokenizer, AutoModelForTokenClassification, AutoModelForSequenceClassification
from train import export_model_to_onnx
import tempfile
import os


def test_onnx_inference(onnx_path, tokenizer, test_text="Je veux aller de Paris à Lyon"):
    """Test inference with ONNX Runtime"""
    try:
        import onnxruntime as ort

        print(f"\n[INFO] Testing ONNX inference with: '{test_text}'")

        # Create session
        session = ort.InferenceSession(onnx_path, providers=['CPUExecutionProvider'])

        # Prepare inputs
        inputs = tokenizer(test_text, return_tensors="np", padding=True, truncation=True, max_length=128)
        ort_inputs = {
            "input_ids": inputs["input_ids"].astype(np.int64),
            "attention_mask": inputs["attention_mask"].astype(np.int64)
        }

        # Run inference
        ort_outputs = session.run(None, ort_inputs)
        logits = ort_outputs[0]

        print(f"[OK] ONNX inference successful")
        print(f"  Output shape: {logits.shape}")
        print(f"  Output range: [{logits.min():.4f}, {logits.max():.4f}]")

        return True

    except ImportError:
        print("[WARN] onnxruntime not installed, skipping inference test")
        print("       Install with: pip install onnxruntime")
        return None
    except Exception as e:
        print(f"[ERROR] ONNX inference failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_ner_export():
    """Test NER model export to ONNX"""
    print("\n" + "="*70)
    print("TEST: NER MODEL ONNX EXPORT")
    print("="*70)

    try:
        # Load model
        model_name = "camembert-base"
        print(f"[INFO] Loading model: {model_name}")
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = AutoModelForTokenClassification.from_pretrained(
            model_name,
            num_labels=15  # Example: O + 7 entity types with B-/I-
        )

        # Export to ONNX
        with tempfile.TemporaryDirectory() as tmp_dir:
            onnx_path = os.path.join(tmp_dir, "ner_test.onnx")

            success = export_model_to_onnx(
                model,
                tokenizer,
                onnx_path,
                task_type="token-classification",
                opset_version=17
            )

            if not success:
                print("[FAIL] Export failed")
                return False

            # Test inference
            result = test_onnx_inference(onnx_path, tokenizer)

            if result is False:
                print("[FAIL] Inference test failed")
                return False
            elif result is None:
                print("[SKIP] Inference test skipped (onnxruntime not installed)")
                return True
            else:
                print("[PASS] NER model ONNX export test passed")
                return True

    except Exception as e:
        print(f"[ERROR] Test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_intent_export():
    """Test Intent model export to ONNX"""
    print("\n" + "="*70)
    print("TEST: INTENT MODEL ONNX EXPORT")
    print("="*70)

    try:
        # Load model
        model_name = "camembert-base"
        print(f"[INFO] Loading model: {model_name}")
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = AutoModelForSequenceClassification.from_pretrained(
            model_name,
            num_labels=2  # Example: trajet, other
        )

        # Export to ONNX
        with tempfile.TemporaryDirectory() as tmp_dir:
            onnx_path = os.path.join(tmp_dir, "intent_test.onnx")

            success = export_model_to_onnx(
                model,
                tokenizer,
                onnx_path,
                task_type="sequence-classification",
                opset_version=17
            )

            if not success:
                print("[FAIL] Export failed")
                return False

            # Test inference
            result = test_onnx_inference(onnx_path, tokenizer)

            if result is False:
                print("[FAIL] Inference test failed")
                return False
            elif result is None:
                print("[SKIP] Inference test skipped (onnxruntime not installed)")
                return True
            else:
                print("[PASS] Intent model ONNX export test passed")
                return True

    except Exception as e:
        print(f"[ERROR] Test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    print("\n" + "="*70)
    print("ONNX EXPORT TEST SUITE")
    print("="*70)

    results = {
        "NER": test_ner_export(),
        "Intent": test_intent_export()
    }

    print("\n" + "="*70)
    print("TEST RESULTS")
    print("="*70)
    for test_name, result in results.items():
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status} - {test_name}")

    all_passed = all(results.values())
    if all_passed:
        print("\n[OK] All tests passed!")
    else:
        print("\n[ERROR] Some tests failed")
        exit(1)


if __name__ == "__main__":
    main()
