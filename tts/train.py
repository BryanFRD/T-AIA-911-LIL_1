"""
Générateur de modèle TTS factice pour tester la pipeline CI/CD

Ce script crée un modèle dummy sans entraînement réel
pour permettre de tester le packaging et le déploiement
"""

import os
import sys
import torch
import torch.nn as nn
from pathlib import Path
import mlflow

# Ajouter le répertoire parent au path
sys.path.insert(0, str(Path(__file__).parent.parent))

from common.mlflow_utils import setup_mlflow, start_run


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

    # Setup MLflow
    experiment_name = os.environ.get("MLFLOW_EXPERIMENT_NAME", "tts")
    setup_mlflow(experiment_name, enable_system_metrics=False)

    with start_run("dummy-tts-ci"):
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

        # Log model to MLflow (PyTorch native - no TorchScript needed)
        mlflow.pytorch.log_model(
            model,
            artifact_path="model",
            registered_model_name="tts"
        )

        # Log artifacts
        mlflow.log_artifacts(output_dir)

        print("\n[OK] TTS model saved to MLflow")
        print("     - Registered in MLflow Model Registry as 'tts'")
        print("     - Local artifacts: model.pt")

    print("\n" + "="*70)
    print("GÉNÉRATION TERMINÉE")
    print("="*70)



if __name__ == "__main__":
    main()
