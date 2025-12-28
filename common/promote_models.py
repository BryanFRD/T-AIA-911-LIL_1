"""
Script de promotion des modèles MLflow

Assigne l'alias 'champion' aux modèles (dernière version ou version spécifique).
Utilisé automatiquement par la CI après chaque training.

Usage:
    # Automatique en CI - dernière version
    python common/promote_models.py --models nlu-ner nlu-intent

    # Manuel - version spécifique (rollback)
    python common/promote_models.py --models nlu-ner --version 7
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
  # Promotion automatique en production (CI) - dernière version
  python common/promote_models.py --models nlu-ner nlu-intent

  # Rollback - promouvoir version précédente
  python common/promote_models.py --models nlu-ner --version 7

Workflow:
  1. Training CI → auto-promote dernière version à 'champion'
  2. Production → charge 'models:/nlu-ner@champion'
  3. Rollback → promote version précédente à 'champion'
        """
    )
    parser.add_argument(
        "--models",
        nargs="+",
        required=True,
        help="Noms des modèles à promouvoir (ex: nlu-ner nlu-intent)"
    )
    parser.add_argument(
        "--version",
        type=int,
        default=None,
        help="Version spécifique à promouvoir (default: dernière version)"
    )

    args = parser.parse_args()

    print("\n" + "="*70)
    print("PROMOTION DES MODÈLES À 'CHAMPION'")
    print("="*70)
    print(f"Modèles: {', '.join(args.models)}")
    if args.version:
        print(f"Version: {args.version}")
    else:
        print("Version: dernière")
    print()

    promote_models(args.models, args.version)

    print("\n" + "="*70)
    print("PROMOTION TERMINÉE")
    print("="*70)


if __name__ == "__main__":
    main()
