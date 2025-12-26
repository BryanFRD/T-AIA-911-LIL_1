"""
MLflow utilities for training NLU, STT and TTS models
"""
import os
import mlflow
from typing import Optional


def setup_mlflow(experiment_name: str, enable_system_metrics: bool = True) -> None:
    """
    Configure MLflow with tracking URI and experiment.

    Args:
        experiment_name: MLflow experiment name
        enable_system_metrics: Enable system metrics logging (CPU, GPU, RAM)
    """
    # Configure tracking URI
    tracking_uri = os.environ.get("MLFLOW_TRACKING_URI", "https://mlflow.bryan-ferrando.fr")
    mlflow.set_tracking_uri(tracking_uri)

    # Handle experiment (with support for deleted experiments)
    try:
        mlflow.set_experiment(experiment_name)
    except mlflow.exceptions.MlflowException as e:
        if "deleted" in str(e).lower():
            # Experiment exists but is deleted - restore it
            print(f"[WARN] Experiment '{experiment_name}' is deleted, restoring it...")
            client = mlflow.tracking.MlflowClient()

            # Get experiment ID by name
            experiment = client.get_experiment_by_name(experiment_name)
            if experiment:
                # Restore the deleted experiment
                client.restore_experiment(experiment.experiment_id)
                print(f"[OK] Experiment '{experiment_name}' restored")
                mlflow.set_experiment(experiment_name)
            else:
                # If not found, create new one
                client.create_experiment(experiment_name)
                mlflow.set_experiment(experiment_name)
        else:
            raise

    # Clean up any active run
    if mlflow.active_run():
        mlflow.end_run()

    # Enable system metrics logging (CPU, GPU, RAM, etc.)
    if enable_system_metrics:
        mlflow.enable_system_metrics_logging()


def start_run(run_name: str, source_name: Optional[str] = None) -> mlflow.ActiveRun:
    """
    Start an MLflow run.

    Args:
        run_name: Run name
        source_name: Optional custom source name (e.g., "GitLab CI", "Local Training")

    Returns:
        MLflow run context
    """
    tags = {}
    if source_name:
        tags["mlflow.source.name"] = source_name

    return mlflow.start_run(run_name=run_name, tags=tags if tags else None)


def log_training_config(config: dict, device: str) -> None:
    """
    Log training configuration parameters to MLflow.

    Args:
        config: Configuration dictionary
        device: Device used (cuda/cpu)
    """
    mlflow.log_param("device", device)

    # Log tokenizer config
    if "tokenizer" in config:
        for key, value in config["tokenizer"].items():
            mlflow.log_param(f"tokenizer_{key}", value)

    # Log training config
    if "training" in config:
        for key, value in config["training"].items():
            mlflow.log_param(f"training_{key}", value)


def log_model_artifact(model_path: str, artifact_path: Optional[str] = None) -> None:
    """
    Log a model as an artifact in MLflow.

    Args:
        model_path: Local model path
        artifact_path: Destination path in MLflow (optional)
    """
    mlflow.log_artifact(model_path, artifact_path)


def log_metrics(metrics: dict, step: Optional[int] = None) -> None:
    """
    Log metrics to MLflow.

    Args:
        metrics: Metrics dictionary {name: value}
        step: Optional step number
    """
    mlflow.log_metrics(metrics, step=step)


def log_pytorch_model(model, artifact_path: str = "model", registered_model_name: Optional[str] = None) -> None:
    """
    Log a PyTorch model to MLflow.

    Args:
        model: PyTorch model to log
        artifact_path: Path within the run's artifact directory (default: "model")
        registered_model_name: If provided, register the model with this name in the MLflow Model Registry
    """
    mlflow.pytorch.log_model(
        model,
        artifact_path=artifact_path,
        registered_model_name=registered_model_name
    )


def end_run() -> None:
    """End the active MLflow run."""
    mlflow.end_run()
