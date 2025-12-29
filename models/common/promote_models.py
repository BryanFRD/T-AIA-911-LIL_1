"""
Script de promotion des modèles MLflow

Assigne l'alias 'champion' aux modèles (dernière version ou version spécifique).
Utilisé automatiquement par la CI après chaque training.

Usage:
    # Automatique en CI - dernière version
    python common/promote_models.py --models nlu-ner-onnx nlu-intent-onnx

    # Manuel - version spécifique (rollback)
    python common/promote_models.py --models nlu-ner-onnx --version 7

    # Promouvoir STT/TTS lite et full en local
    python common/promote_models.py --base-models stt-onnx tts-onnx --variants lite full
"""

import argparse
from mlflow import MlflowClient


def promote_models(model_names: list[str], version: int = None) -> None:
    """
    Assigne l'alias 'champion' à des versions de modèles dans MLflow

    Args:
        model_names: Liste des noms de modèles à promouvoir
        version: Numéro de version spécifique (None = dernière version)
    """
    client = MlflowClient()

    for model_name in model_names:
        try:
            # Récupérer toutes les versions du modèle
            versions = client.search_model_versions(f"name='{model_name}'")

            if not versions:
                print(f"[WARN] Aucune version trouvée pour le modèle '{model_name}'")
                continue

            # Choisir la version à promouvoir
            if version is not None:
                # Version spécifique demandée
                target_version = next((v for v in versions if int(v.version) == version), None)
                if not target_version:
                    print(f"[ERROR] Version {version} non trouvée pour '{model_name}'")
                    continue
                version_number = str(version)
            else:
                # Dernière version
                latest_version = sorted(versions, key=lambda x: int(x.version), reverse=True)[0]
                version_number = latest_version.version

            # Assigner l'alias 'champion'
            client.set_registered_model_alias(
                name=model_name,
                alias="champion",
                version=version_number
            )

            print(f"[OK] {model_name} v{version_number} -> alias 'champion'")

            # Afficher les alias actuels pour ce modèle
            current_version = client.get_model_version(model_name, version_number)
            if hasattr(current_version, 'aliases') and current_version.aliases:
                print(f"     Aliases actifs: {', '.join(current_version.aliases)}")

        except Exception as e:
            print(f"[ERROR] Échec de la promotion pour '{model_name}': {e}")
            raise


def main():
    parser = argparse.ArgumentParser(
        description="Promouvoir les modèles MLflow avec l'alias 'champion'",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemples:
  # Promotion automatique en CI - modèles spécifiques
  python common/promote_models.py --models nlu-ner-onnx nlu-intent-onnx

  # Promotion en local - toutes les variantes STT/TTS
  python common/promote_models.py --base-models stt-onnx tts-onnx --variants lite full

  # Rollback - promouvoir version précédente
  python common/promote_models.py --models nlu-ner-onnx --version 7

Workflow:
  1. Training CI → auto-promote dernière version à 'champion'
  2. Production → charge 'models:/nlu-ner-onnx@champion'
  3. Rollback → promote version précédente à 'champion'
        """
    )
    parser.add_argument(
        "--models",
        nargs="+",
        help="Noms complets des modèles à promouvoir (ex: nlu-ner-onnx nlu-intent-onnx)"
    )
    parser.add_argument(
        "--base-models",
        nargs="+",
        help="Noms de base des modèles (ex: stt-onnx tts-onnx). À combiner avec --variants"
    )
    parser.add_argument(
        "--variants",
        nargs="+",
        choices=["lite", "full"],
        help="Variantes à promouvoir (lite, full). Utilisé avec --base-models"
    )
    parser.add_argument(
        "--version",
        type=int,
        default=None,
        help="Version spécifique à promouvoir (default: dernière version)"
    )

    args = parser.parse_args()

    # Construire la liste des modèles
    model_names = []

    if args.models:
        model_names.extend(args.models)

    if args.base_models and args.variants:
        for base_model in args.base_models:
            for variant in args.variants:
                model_names.append(f"{base_model}-{variant}")
    elif args.base_models or args.variants:
        parser.error("--base-models et --variants doivent être utilisés ensemble")

    if not model_names:
        parser.error("Vous devez spécifier --models OU (--base-models + --variants)")

    print("\n" + "="*70)
    print("PROMOTION DES MODÈLES À 'CHAMPION'")
    print("="*70)
    print(f"Modèles: {', '.join(model_names)}")
    if args.version:
        print(f"Version: {args.version}")
    else:
        print("Version: dernière")
    print()

    promote_models(model_names, args.version)

    print("\n" + "="*70)
    print("PROMOTION TERMINÉE")
    print("="*70)


if __name__ == "__main__":
    main()
