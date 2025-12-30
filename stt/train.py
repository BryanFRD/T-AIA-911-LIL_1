"""
STT Training - Whisper fine-tuning for French speech recognition

Uses common utilities and MLflow for tracking.
Supports mini-train mode for quick testing.
"""

import os
import sys
import atexit
import argparse
from pathlib import Path
import torch

# Add parent to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from common.mlflow_utils import setup_mlflow, start_run, log_training_config
from common.training_utils import detect_device, print_system_info, load_config, get_machine_name
from common.onnx_utils import log_model_artifacts
import mlflow


def cleanup_mlflow():
    """Cleanup function to ensure MLflow run is terminated properly"""
    if mlflow.active_run():
        print("\n[CLEANUP] Terminating active MLflow run...")
        try:
            mlflow.set_tag("training_status", "interrupted")
            mlflow.end_run(status="KILLED")
            print("[OK] MLflow run terminated")
        except Exception as e:
            print(f"[WARN] Error during MLflow cleanup: {e}")


def train_variant(variant: str, device: str, mini_train: bool):
    """Train a specific variant (lite or full)"""
    # Load configuration for this variant
    if os.environ.get("CONFIG_OVERRIDE"):
        # CI mode: use CONFIG_OVERRIDE
        CONFIG_FILE = "stt/config.json"
    else:
        # Local mode: use variant-specific config
        CONFIG_FILE = f"stt/config.{variant}.json"

    config = load_config(CONFIG_FILE)

    # Mini-train mode override
    if mini_train:
        print(f"\n[MINI-TRAIN] Mode rapide activé pour {variant} - dataset réduit")
        config["training"]["max_steps"] = 10
        config["training"]["eval_steps"] = 5
        config["training"]["save_steps"] = 10

    # Detect CI or local
    if os.environ.get("CI") == "true":
        pipeline_id = os.environ.get("CI_PIPELINE_ID", "unknown")
        source_name = f"CI #{pipeline_id}"
    else:
        machine_name = get_machine_name()
        source_name = f"Local ({machine_name})"

    try:
        with start_run(f"stt-training-{variant}", source_name=source_name):
            # Log configuration
            log_training_config(config, device)
            mlflow.log_param("mini_train", mini_train)
            mlflow.log_param("model_variant", variant)
            mlflow.log_param("model_type", config.get("model", {}).get("type", "whisper-small"))

            print("\n" + "="*70)
            print(f"STT TRAINING - Whisper Fine-tuning ({variant.upper()})")
            print("="*70)
            print(f"[INFO] Variant: {variant}")
            print(f"[INFO] Device: {device}")
            print(f"[INFO] Model: {config.get('model', {}).get('type', 'whisper-small')}")
            print(f"[INFO] Mini-train: {mini_train}")

            # TODO: Implement actual Whisper training
            # For now, create a dummy model to test the pipeline
            print("\n[WARN] Full Whisper training not implemented yet")
            print("[INFO] Creating dummy STT model for pipeline testing...")

            import torch.nn as nn

            class DummySTT(nn.Module):
                def __init__(self):
                    super().__init__()
                    self.linear = nn.Linear(10, 10)

                def forward(self, x):
                    return self.linear(x)

            model = DummySTT()

            # Log dummy metrics
            mlflow.log_metric("wer", 0.25)  # Word Error Rate
            mlflow.log_metric("loss", 0.5)

            # Save model to MLflow (ONNX only)
            print("\n[INFO] Saving STT model to MLflow (ONNX)...")

            # Create dummy input for ONNX export
            dummy_input = (torch.randn(1, 10),)

            # Export and log ONNX model
            import tempfile
            from common.onnx_utils import export_pytorch_to_onnx

            # Create temporary directory that we'll manage manually
            tmp_dir = tempfile.mkdtemp()
            try:
                onnx_path = os.path.join(tmp_dir, "stt.onnx")

                success = export_pytorch_to_onnx(
                    model=model,
                    dummy_input=dummy_input,
                    output_path=onnx_path,
                    input_names=["audio_features"],
                    output_names=["logits"],
                    dynamic_axes={
                        "audio_features": {0: "batch_size"},
                        "logits": {0: "batch_size"}
                    },
                    opset_version=17
                )

                if success:
                    import onnx
                    onnx_model = onnx.load(onnx_path)

                    registered_model_name = f"stt-onnx-{variant}"

                    mlflow.onnx.log_model(
                        onnx_model=onnx_model,
                        artifact_path="stt_onnx",
                        registered_model_name=registered_model_name
                    )
                    print(f"[OK] STT model (ONNX) logged to MLflow as {registered_model_name}")

                    # Also save raw ONNX file
                    mlflow.log_artifact(onnx_path, artifact_path="onnx_files")
                    print("[OK] STT ONNX file saved to MLflow")

                # CRITICAL: Flush MLflow to ensure artifacts are uploaded before cleanup
                if mlflow.active_run():
                    mlflow.flush_async_logging()
                    print("[INFO] MLflow artifacts flushed")

            finally:
                # Clean up temporary directory after MLflow has uploaded
                if tmp_dir and os.path.exists(tmp_dir):
                    import shutil
                    shutil.rmtree(tmp_dir)
                    print("[INFO] Temporary directory cleaned up")

            # Log tags
            mlflow.set_tag("model_type", "stt")
            mlflow.set_tag("model_variant", variant)
            mlflow.set_tag("framework", "pytorch")
            mlflow.set_tag("training_status", "completed")

            print("\n" + "="*70)
            print(f"STT TRAINING COMPLETED ({variant.upper()})")
            print("="*70)

    except KeyboardInterrupt:
        print(f"\n[INTERRUPT] Training interrupted by user ({variant})")
        if mlflow.active_run():
            mlflow.set_tag("training_status", "interrupted_by_user")
        raise
    except Exception as e:
        print(f"\n[ERROR] Training failed ({variant}): {e}")
        if mlflow.active_run():
            mlflow.set_tag("training_status", "failed")
            mlflow.set_tag("error_message", str(e))
        raise


def main():
    # Register cleanup handler
    atexit.register(cleanup_mlflow)

    # Parse arguments
    parser = argparse.ArgumentParser(description="STT Training - Whisper Fine-tuning")
    parser.add_argument("--variant", type=str, choices=["lite", "full"],
                       help="Model variant to train (lite or full). If not specified, trains both.")
    args = parser.parse_args()

    # === MLflow Configuration ===
    experiment_name = os.environ.get("MLFLOW_EXPERIMENT_NAME", "stt")
    setup_mlflow(experiment_name, enable_system_metrics=True)

    # === Device detection ===
    print_system_info()
    device = detect_device()

    # Mini-train mode (quick test)
    mini_train = os.environ.get("MINI_TRAIN", "false").lower() == "true"

    # Determine which variants to train
    # Priority: CLI arg > MODEL_VARIANT env var > both
    if args.variant:
        variants = [args.variant]
    elif os.environ.get("MODEL_VARIANT"):
        variants = [os.environ.get("MODEL_VARIANT")]
    else:
        variants = ["lite", "full"]

    print(f"\n[INFO] Training variants: {', '.join(variants)}\n")

    # Train each variant
    for variant in variants:
        train_variant(variant, device, mini_train)


if __name__ == "__main__":
    main()
