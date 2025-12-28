"""
TTS Training - Text-to-Speech model training for French

Uses common utilities and MLflow for tracking.
Supports mini-train mode for quick testing.
"""

import os
import sys
import atexit
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


def main():
    # Register cleanup handler
    atexit.register(cleanup_mlflow)

    # === MLflow Configuration ===
    experiment_name = os.environ.get("MLFLOW_EXPERIMENT_NAME", "tts")
    setup_mlflow(experiment_name, enable_system_metrics=True)

    # === Device detection ===
    print_system_info()
    device = detect_device()

    # === Load configuration ===
    CONFIG_FILE = "tts/config.json"
    config = load_config(CONFIG_FILE)

    # Mini-train mode (quick test)
    mini_train = os.environ.get("MINI_TRAIN", "false").lower() == "true"
    if mini_train:
        print("\n[MINI-TRAIN] Mode rapide activé - dataset réduit")
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
        with start_run("tts-training", source_name=source_name):
            # Log configuration
            log_training_config(config, device)
            mlflow.log_param("mini_train", mini_train)
            mlflow.log_param("model_type", config.get("model", {}).get("type", "vits"))

            print("\n" + "="*70)
            print("TTS TRAINING - Text-to-Speech")
            print("="*70)
            print(f"[INFO] Device: {device}")
            print(f"[INFO] Model: {config.get('model', {}).get('type', 'vits')}")
            print(f"[INFO] Mini-train: {mini_train}")

            # TODO: Implement actual TTS training (VITS, FastSpeech2, etc.)
            # For now, create a dummy model to test the pipeline
            print("\n[WARN] Full TTS training not implemented yet")
            print("[INFO] Creating dummy TTS model for pipeline testing...")

            import torch.nn as nn

            class DummyTTS(nn.Module):
                def __init__(self):
                    super().__init__()
                    self.linear = nn.Linear(10, 10)

                def forward(self, x):
                    return self.linear(x)

            model = DummyTTS()

            # Log dummy metrics
            mlflow.log_metric("mel_loss", 0.15)
            mlflow.log_metric("duration_loss", 0.08)
            mlflow.log_metric("total_loss", 0.23)

            # Save model to MLflow (PyTorch + ONNX)
            print("\n[INFO] Saving TTS model to MLflow...")

            # Create dummy input for ONNX export (adjust shape for real TTS model)
            dummy_input = (torch.randn(1, 10),)

            log_model_artifacts(
                mlflow=mlflow,
                model=model,
                model_path=None,  # No HuggingFace format for dummy model
                artifact_base_name="tts",
                registered_model_name="tts",
                export_onnx=True,
                onnx_dummy_input=dummy_input,
                onnx_input_names=["text_features"],
                onnx_output_names=["mel_spectrogram"],
                onnx_dynamic_axes={
                    "text_features": {0: "batch_size"},
                    "mel_spectrogram": {0: "batch_size"}
                },
                opset_version=17
            )
            print("[OK] TTS model artifacts saved to MLflow")

            # Log tags
            mlflow.set_tag("model_type", "tts")
            mlflow.set_tag("framework", "pytorch")
            mlflow.set_tag("training_status", "completed")

            print("\n" + "="*70)
            print("TTS TRAINING COMPLETED")
            print("="*70)

    except KeyboardInterrupt:
        print("\n[INTERRUPT] Training interrupted by user")
        if mlflow.active_run():
            mlflow.set_tag("training_status", "interrupted_by_user")
        raise
    except Exception as e:
        print(f"\n[ERROR] Training failed: {e}")
        if mlflow.active_run():
            mlflow.set_tag("training_status", "failed")
            mlflow.set_tag("error_message", str(e))
        raise


if __name__ == "__main__":
    main()
