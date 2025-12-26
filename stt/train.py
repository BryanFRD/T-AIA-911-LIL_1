"""
Générateur de modèle STT factice pour tester la pipeline CI/CD

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
from common.training_utils import save_config


class DummySTTModel(nn.Module):
    """Modèle STT factice"""
    def __init__(self):
        super().__init__()
        self.linear = nn.Linear(10, 10)

    def forward(self, x):
        return self.linear(x)


def main():
    print("="*70)
    print("GÉNÉRATION MODÈLE STT DUMMY (POUR TEST CI/CD)")
    print("="*70)

    # Setup MLflow
    experiment_name = os.environ.get("MLFLOW_EXPERIMENT_NAME", "stt")
    setup_mlflow(experiment_name, enable_system_metrics=False)

    with start_run("dummy-stt-ci"):
        output_dir = "stt/models/model_stt"
        os.makedirs(output_dir, exist_ok=True)

        # Créer un modèle factice
        model = DummySTTModel()

        # Log params
        mlflow.log_param("model_type", "dummy_stt")
        mlflow.log_param("sample_rate", 16000)
        mlflow.log_param("vocab_size", 100)
        mlflow.log_param("is_dummy", True)

        # Sauvegarder le modèle
        torch.save({
            'model_state_dict': model.state_dict(),
            'architecture': 'dummy',
            'description': 'Modèle STT factice pour test pipeline'
        }, f"{output_dir}/model.pt")

        # Créer une config factice
        config = {
            "model_type": "dummy_stt",
            "sample_rate": 16000,
            "vocab_size": 100,
            "description": "Dummy STT model for CI/CD testing"
        }
        save_config(config, f"{output_dir}/config.json")

        # Créer un fichier de métadonnées
        metadata = {
            "framework": "pytorch",
            "task": "speech-to-text",
            "language": "fr",
            "is_dummy": True
        }
        save_config(metadata, f"{output_dir}/metadata.json")

        # Log model to MLflow (PyTorch native - no TorchScript needed)
        mlflow.pytorch.log_model(
            model,
            artifact_path="model",
            registered_model_name="stt"
        )

        # Log artifacts
        mlflow.log_artifacts(output_dir)

        print("\n[OK] STT model saved to MLflow")
        print("     - Registered in MLflow Model Registry as 'stt'")
        print("     - Local artifacts: model.pt, config.json, metadata.json")
        print("\n" + "="*70)
        print("GÉNÉRATION TERMINÉE")
        print("="*70)


if __name__ == "__main__":
    main()
