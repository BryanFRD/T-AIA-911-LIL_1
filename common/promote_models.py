"""
Script de promotion des modèles MLflow

Assigne l'alias "latest" aux dernières versions enregistrées dans MLflow.
Utilisé en CI après chaque entraînement pour marquer les modèles récents.

Usage:
    python common/promote_models.py --models nlu-ner nlu-intent
    python common/promote_models.py --models stt
    python common/promote_models.py --models tts
"""

import argparse
from mlflow import MlflowClient


def promote_to_latest(model_names: list[str]) -> None:
    """
    Assigne l'alias 'latest' aux dernières versions des modèles spécifiés

    Args:
        model_names: Liste des noms de modèles à promouvoir (ex: ["nlu-ner", "nlu-intent"])
    """
    client = MlflowClient("https://mlflow.bryan-ferrando.fr")

    for model_name in model_names:
        try:
            # Récupérer toutes les versions du modèle
            versions = client.search_model_versions(f"name='{model_name}'")

            if not versions:
                print(f"[WARN] Aucune version trouvée pour le modèle '{model_name}'")
                continue

            # Trier par version (la plus récente en premier)
            latest_version = sorted(versions, key=lambda x: int(x.version), reverse=True)[0]
            version_number = latest_version.version

            # Assigner l'alias "latest"
            client.set_registered_model_alias(
                name=model_name,
                alias="latest",
                version=version_number
            )

            print(f"[OK] {model_name} v{version_number} -> alias 'latest'")

        except Exception as e:
            print(f"[ERROR] Échec de la promotion pour '{model_name}': {e}")
            raise


def main():
    parser = argparse.ArgumentParser(description="Promouvoir les modèles MLflow avec l'alias 'latest'")
    parser.add_argument(
        "--models",
        nargs="+",
        required=True,
        help="Noms des modèles à promouvoir (ex: NLU-NER NLU-Intent)"
    )

    args = parser.parse_args()

    print("\n" + "="*70)
    print("PROMOTION DES MODÈLES")
    print("="*70)
    print(f"Modèles: {', '.join(args.models)}")
    print()

    promote_to_latest(args.models)

    print("\n" + "="*70)
    print("PROMOTION TERMINÉE")
    print("="*70)


if __name__ == "__main__":
    main()
