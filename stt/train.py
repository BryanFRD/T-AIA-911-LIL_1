"""
Générateur de modèle STT factice pour tester la pipeline CI/CD

Ce script crée un modèle dummy sans entraînement réel
pour permettre de tester le packaging et le déploiement
"""

import os
import json
import torch
import torch.nn as nn


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

    output_dir = "stt/models/model_stt"
    os.makedirs(output_dir, exist_ok=True)

    # Créer un modèle factice
    model = DummySTTModel()

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

    with open(f"{output_dir}/config.json", "w") as f:
        json.dump(config, f, indent=2)

    # Créer un fichier de métadonnées
    metadata = {
        "framework": "pytorch",
        "task": "speech-to-text",
        "language": "fr",
        "is_dummy": True
    }

    with open(f"{output_dir}/metadata.json", "w") as f:
        json.dump(metadata, f, indent=2)

    print(f"\n[OK] Modèle STT dummy créé dans {output_dir}")
    print("     - model.pt")
    print("     - config.json")
    print("     - metadata.json")
    print("\n" + "="*70)
    print("GÉNÉRATION TERMINÉE")
    print("="*70)


if __name__ == "__main__":
    main()
