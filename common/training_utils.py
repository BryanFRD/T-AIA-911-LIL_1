"""
Common utilities for training NLU, STT and TTS models
"""
import os
import json
import socket
import platform
import torch
from pathlib import Path
from typing import Dict, Tuple, Any


def load_env():
    """
    Load environment variables from .env file in project root.
    This loads AWS credentials and MLflow configuration.
    """
    try:
        from dotenv import load_dotenv

        # Find .env file in project root (parent of common/)
        env_file = Path(__file__).parent.parent / ".env"

        if env_file.exists():
            load_dotenv(env_file)
            print(f"[INFO] Environment variables loaded from {env_file}")
        else:
            print(f"[WARN] No .env file found at {env_file}")
    except ImportError:
        print("[WARN] python-dotenv not installed, skipping .env loading")


def get_machine_name(env_var: str = "MACHINE_NAME") -> str:
    """
    Get the machine name from environment variable or system hostname.

    Args:
        env_var: Environment variable name to check first (default: "MACHINE_NAME")

    Returns:
        Machine name (from env var or hostname)
    """
    # Try environment variable first
    machine_name = os.environ.get(env_var)
    if machine_name:
        return machine_name

    # Fallback to socket hostname (works on Windows, Linux, macOS)
    try:
        return socket.gethostname()
    except Exception:
        # Ultimate fallback
        return f"{platform.system()}-{platform.node()}"


def detect_device() -> str:
    """
    Automatically detect available device (CUDA or CPU).

    Returns:
        "cuda" if GPU available, otherwise "cpu"
    """
    return "cuda" if torch.cuda.is_available() else "cpu"


def print_system_info() -> None:
    """Print system information (PyTorch, CUDA)."""
    print("=" * 70)
    print("SYSTEM CONFIGURATION")
    print("=" * 70)
    print(f"PyTorch version: {torch.__version__}")
    print(f"CUDA available: {torch.cuda.is_available()}")

    if torch.cuda.is_available():
        print(f"CUDA version: {torch.version.cuda}")
        print(f"GPU: {torch.cuda.get_device_name(0)}")

    device = detect_device()
    print(f"\nSelected device: {device.upper()}")
    print("=" * 70 + "\n")


def load_config(config_path: str) -> Dict[str, Any]:
    """
    Load a JSON configuration file with support for local overrides.

    Priority order (highest to lowest):
    1. CONFIG_OVERRIDE env var (e.g., CONFIG_OVERRIDE=config.ci.json)
    2. config.local.json (not versioned, for local development)
    3. config.json (base configuration, versioned)

    Args:
        config_path: Path to config.json file (must exist)

    Returns:
        Configuration dictionary (merged base + overrides)

    Raises:
        FileNotFoundError: If config_path doesn't exist

    Environment Variables:
        CONFIG_OVERRIDE: Path to override config file (e.g., "nlu/config.ci.json")
    """
    # Load base config (required)
    if not os.path.exists(config_path):
        raise FileNotFoundError(f"Configuration file not found: {config_path}")

    with open(config_path) as f:
        config = json.load(f)

    # Check for CONFIG_OVERRIDE environment variable (highest priority)
    override_path = os.environ.get('CONFIG_OVERRIDE')
    if override_path:
        if os.path.exists(override_path):
            print(f"[INFO] Loading config override from environment: {override_path}")
            with open(override_path) as f:
                override_config = json.load(f)
            config = _deep_merge(config, override_config)
        else:
            print(f"[WARN] CONFIG_OVERRIDE set to '{override_path}' but file not found, ignoring")
    else:
        # Check for local override config (optional, for local development)
        local_config_path = config_path.replace('.json', '.local.json')
        if os.path.exists(local_config_path):
            print(f"[INFO] Loading local config overrides from {local_config_path}")
            with open(local_config_path) as f:
                local_config = json.load(f)
            config = _deep_merge(config, local_config)

    return config


def _deep_merge(base: Dict[str, Any], override: Dict[str, Any]) -> Dict[str, Any]:
    """
    Deep merge two dictionaries.

    Args:
        base: Base dictionary
        override: Override dictionary (values take precedence)

    Returns:
        Merged dictionary
    """
    result = base.copy()
    for key, value in override.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = _deep_merge(result[key], value)
        else:
            result[key] = value
    return result


def ensure_dir(directory: str) -> None:
    """
    Create a directory if it doesn't exist.

    Args:
        directory: Directory path to create
    """
    os.makedirs(directory, exist_ok=True)


def save_config(config: Dict[str, Any], output_path: str) -> None:
    """
    Save configuration to a JSON file.

    Args:
        config: Configuration dictionary
        output_path: Output file path
    """
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(config, f, indent=2, ensure_ascii=False)


def print_dataset_info(total: int, train: int, val: int) -> None:
    """
    Print dataset information.

    Args:
        total: Total number of examples
        train: Number of training examples
        val: Number of validation examples
    """
    print(f"Dataset loaded: {total} examples")
    print(f"  Train: {train} examples")
    print(f"  Validation: {val} examples")
    print()
