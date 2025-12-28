"""
Utilitaires pour l'export ONNX des modèles PyTorch

Utilisé par NLU, STT, TTS pour exporter les modèles en format ONNX
"""

import torch
from pathlib import Path


def export_pytorch_to_onnx(
    model,
    dummy_input,
    output_path,
    input_names=None,
    output_names=None,
    dynamic_axes=None,
    opset_version=17
):
    """
    Export un modèle PyTorch vers ONNX

    Args:
        model: Modèle PyTorch à exporter
        dummy_input: Tuple d'inputs exemple pour le traçage (e.g., (input_tensor,))
        output_path: Chemin de sortie pour le fichier .onnx
        input_names: Liste des noms d'entrée (default: ["input"])
        output_names: Liste des noms de sortie (default: ["output"])
        dynamic_axes: Dict des axes dynamiques (pour batch_size variable, etc.)
        opset_version: Version opset ONNX (default: 17)

    Returns:
        bool: True si succès, False sinon
    """
    try:
        from torch.onnx import export as onnx_export

        print(f"[INFO] Exporting model to ONNX (opset {opset_version})...")

        # Valeurs par défaut
        if input_names is None:
            input_names = ["input"]
        if output_names is None:
            output_names = ["output"]
        if dynamic_axes is None:
            dynamic_axes = {
                "input": {0: "batch_size"},
                "output": {0: "batch_size"}
            }

        # Set model to eval mode
        model.eval()

        # Export to ONNX
        with torch.no_grad():
            onnx_export(
                model,
                dummy_input,
                output_path,
                input_names=input_names,
                output_names=output_names,
                dynamic_axes=dynamic_axes,
                opset_version=opset_version,
                do_constant_folding=True,
                export_params=True
            )

        print(f"[OK] ONNX model exported to {output_path}")

        # Verify ONNX model
        try:
            import onnx
            onnx_model = onnx.load(output_path)
            onnx.checker.check_model(onnx_model)
            print("[OK] ONNX model validation passed")
        except ImportError:
            print("[WARN] onnx package not installed, skipping validation")
        except Exception as e:
            print(f"[WARN] ONNX validation failed: {e}")

        return True

    except Exception as e:
        print(f"[ERROR] Failed to export ONNX model: {e}")
        import traceback
        traceback.print_exc()
        return False


def log_model_artifacts(
    mlflow,
    model,
    model_path,
    artifact_base_name,
    registered_model_name,
    export_onnx=False,
    onnx_dummy_input=None,
    onnx_input_names=None,
    onnx_output_names=None,
    onnx_dynamic_axes=None,
    opset_version=17
):
    """
    Log des artifacts de modèle dans MLflow (PyTorch + optionnellement ONNX)

    Args:
        mlflow: Module mlflow
        model: Modèle PyTorch
        model_path: Chemin temporaire où sauvegarder le modèle (HuggingFace format)
        artifact_base_name: Nom de base pour les artifacts (e.g., "stt")
        registered_model_name: Nom dans le Model Registry (e.g., "stt")
        export_onnx: Si True, exporte aussi en ONNX
        onnx_dummy_input: Input exemple pour ONNX export
        onnx_input_names: Noms des inputs ONNX
        onnx_output_names: Noms des outputs ONNX
        onnx_dynamic_axes: Axes dynamiques ONNX
        opset_version: Version opset ONNX

    Returns:
        None
    """
    import os
    import tempfile

    # Log PyTorch model (creates logged_model artifact)
    mlflow.pytorch.log_model(
        pytorch_model=model,
        artifact_path=f"{artifact_base_name}_pytorch",
        registered_model_name=registered_model_name
    )
    print(f"[OK] {artifact_base_name} model (PyTorch) logged to MLflow")

    # Log HuggingFace format if model_path exists
    if model_path and os.path.exists(model_path):
        mlflow.log_artifacts(model_path, artifact_path=f"{artifact_base_name}_hf")
        print(f"[OK] {artifact_base_name} model (HuggingFace format) saved to MLflow")

    # ONNX export (if enabled)
    if export_onnx and onnx_dummy_input is not None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            onnx_path = os.path.join(tmp_dir, f"{artifact_base_name}.onnx")

            success = export_pytorch_to_onnx(
                model=model,
                dummy_input=onnx_dummy_input,
                output_path=onnx_path,
                input_names=onnx_input_names,
                output_names=onnx_output_names,
                dynamic_axes=onnx_dynamic_axes,
                opset_version=opset_version
            )

            if success:
                # Log as ONNX model
                mlflow.onnx.log_model(
                    onnx_model=onnx_path,
                    artifact_path=f"{artifact_base_name}_onnx"
                )
                print(f"[OK] {artifact_base_name} model (ONNX) logged to MLflow")

                # Also save raw ONNX file
                mlflow.log_artifact(onnx_path, artifact_path="onnx_files")
                print(f"[OK] {artifact_base_name} ONNX file saved to MLflow")
