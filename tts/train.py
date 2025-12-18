"""
Générateur de modèle TTS factice pour tester la pipeline CI/CD

Ce script crée un modèle dummy sans entraînement réel
pour permettre de tester le packaging et le déploiement
"""

import os
import sys
import json
import torch
import torch.nn as nn
from pathlib import Path
import mlflow

# Ajouter le répertoire parent au path
sys.path.insert(0, str(Path(__file__).parent.parent))

from common.save_model import export_simple_model_to_torchscript


class DummyTTSModel(nn.Module):
    """Modèle TTS factice"""
    def __init__(self):
        super().__init__()
        self.linear = nn.Linear(10, 10)

    def forward(self, x):
        return self.linear(x)


def main():
    print("="*70)
    print("GÉNÉRATION MODÈLE TTS DUMMY (POUR TEST CI/CD)")
    print("="*70)

    mlflow.set_tracking_uri(os.environ.get("MLFLOW_TRACKING_URI", "https://mlflow.bryan-ferrando.fr"))
    mlflow.set_experiment(os.environ.get("MLFLOW_EXPERIMENT_NAME", "tts"))

    with mlflow.start_run(run_name="dummy-tts-ci"):
        output_dir = "tts/models/model_tts"
        os.makedirs(output_dir, exist_ok=True)

        model = DummyTTSModel()

        # Log params
        mlflow.log_param("model_type", "dummy_tts")
        mlflow.log_param("sample_rate", 22050)
        mlflow.log_param("is_dummy", True)

        torch.save({
            'model_state_dict': model.state_dict(),
            'architecture': 'dummy',
            'description': 'Modèle TTS factice pour test pipeline'
        }, f"{output_dir}/model.pt")

        export_simple_model_to_torchscript(
            model,
            f"{output_dir}/model_tts_jit.pt",
            input_shape=(1, 10)
        )

        # Log model MLflow
        mlflow.pytorch.log_model(
            model,
            artifact_path="model",
            registered_model_name="tts"
        )

        # Log artifacts
        mlflow.log_artifacts(output_dir)

    print(f"\n[OK] Modèle TTS dummy créé dans {output_dir}")



if __name__ == "__main__":
    main()
