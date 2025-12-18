"""
Générateur de modèle TTS factice pour tester la pipeline CI/CD

Ce script crée un modèle dummy sans entraînement réel
pour permettre de tester le packaging et le déploiement
"""

import os
import json
import torch
import torch.nn as nn


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

    output_dir = "tts/models/model_tts"
    os.makedirs(output_dir, exist_ok=True)

    # Créer un modèle factice
    model = DummyTTSModel()

    # Sauvegarder le modèle
    torch.save({
        'model_state_dict': model.state_dict(),
        'architecture': 'dummy',
        'description': 'Modèle TTS factice pour test pipeline'
    }, f"{output_dir}/model.pt")

    # Créer une config factice
    config = {
        "model_type": "dummy_tts",
        "sample_rate": 22050,
        "speaker_embedding_dim": 256,
        "description": "Dummy TTS model for CI/CD testing"
    }

    with open(f"{output_dir}/config.json", "w") as f:
        json.dump(config, f, indent=2)

    # Créer un fichier de métadonnées
    metadata = {
        "framework": "pytorch",
        "task": "text-to-speech",
        "language": "fr",
        "is_dummy": True
    }

    with open(f"{output_dir}/metadata.json", "w") as f:
        json.dump(metadata, f, indent=2)

    print(f"\n[OK] Modèle TTS dummy créé dans {output_dir}")
    print("     - model.pt")
    print("     - config.json")
    print("     - metadata.json")
    print("\n" + "="*70)
    print("GÉNÉRATION TERMINÉE")
    print("="*70)


if __name__ == "__main__":
    main()
