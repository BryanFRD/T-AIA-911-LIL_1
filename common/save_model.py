"""
Module de sauvegarde et export de modèles pour différents backends

Supporte:
- PyTorch natif (.pt, .pth)
- ONNX (.onnx) - pour DJL Java, TensorRT, etc.
- TorchScript (.pt) - pour production PyTorch
"""

import os
import json
from pathlib import Path
import torch
from typing import Dict, Optional, Union


def save_pytorch_model(model, tokenizer, output_dir: str, config: Optional[Dict] = None):
    """
    Sauvegarde un modèle PyTorch avec son tokenizer et sa config

    Args:
        model: Modèle HuggingFace à sauvegarder
        tokenizer: Tokenizer associé
        output_dir: Répertoire de sortie
        config: Configuration optionnelle (labels, hyperparams, etc.)
    """
    os.makedirs(output_dir, exist_ok=True)

    # Sauvegarder modèle et tokenizer
    model.save_pretrained(output_dir)
    tokenizer.save_pretrained(output_dir)

    # Sauvegarder config additionnelle
    if config:
        with open(os.path.join(output_dir, "labels_config.json"), "w") as f:
            json.dump(config, f, indent=2)

    print(f"[OK] Modèle PyTorch sauvegardé dans {output_dir}")


def export_to_onnx(
    model,
    tokenizer,
    output_path: str,
    task: str = "ner",
    max_length: int = 128,
    opset_version: int = 17,
    optimize: bool = True
):
    """
    Exporte un modèle HuggingFace en ONNX pour DJL Java

    Args:
        model: Modèle HuggingFace à exporter
        tokenizer: Tokenizer associé
        output_path: Chemin du fichier ONNX de sortie (.onnx)
        task: Type de tâche ("ner" pour token classification, "intent" pour sequence classification)
        max_length: Longueur max des séquences
        opset_version: Version ONNX (17 recommandé pour DJL)
        optimize: Appliquer optimisations ONNX Runtime

    Notes DJL Java:
        - DJL supporte ONNX Runtime via ai.djl:onnxruntime
        - Format compatible avec PyTorch ONNX export
        - Batch dynamique supporté
    """
    model.eval()
    device = next(model.parameters()).device

    # Créer inputs factices pour traçage
    dummy_text = "Je veux aller de Paris à Lyon"
    inputs = tokenizer(
        dummy_text,
        return_tensors="pt",
        truncation=True,
        max_length=max_length,
        padding="max_length"
    ).to(device)

    # Noms des inputs/outputs
    input_names = ["input_ids", "attention_mask"]
    output_names = ["logits"]

    # Axes dynamiques pour batch variable
    if task == "ner":
        # Token classification: (batch, seq_len, num_labels)
        dynamic_axes = {
            "input_ids": {0: "batch_size", 1: "seq_len"},
            "attention_mask": {0: "batch_size", 1: "seq_len"},
            "logits": {0: "batch_size", 1: "seq_len"}
        }
    elif task == "intent":
        # Sequence classification: (batch, num_labels)
        dynamic_axes = {
            "input_ids": {0: "batch_size", 1: "seq_len"},
            "attention_mask": {0: "batch_size", 1: "seq_len"},
            "logits": {0: "batch_size"}
        }
    else:
        raise ValueError(f"Task inconnue: {task}")

    # Export ONNX
    with torch.no_grad():
        torch.onnx.export(
            model,
            (inputs["input_ids"], inputs["attention_mask"]),
            output_path,
            input_names=input_names,
            output_names=output_names,
            dynamic_axes=dynamic_axes,
            opset_version=opset_version,
            do_constant_folding=True,  # Optimisation compile-time
            export_params=True,
            verbose=False
        )

    print(f"[OK] Modèle exporté en ONNX: {output_path}")

    # Optimisation ONNX Runtime (optionnel)
    if optimize:
        try:
            import onnx
            from onnxruntime.transformers import optimizer

            # Charger et optimiser
            model_onnx = onnx.load(output_path)
            optimized_model = optimizer.optimize_model(
                output_path,
                model_type='bert',  # CamemBERT est basé sur BERT
                num_heads=12,
                hidden_size=768
            )
            optimized_model.save_model_to_file(output_path)
            print(f"[OK] Modèle ONNX optimisé pour inférence")
        except ImportError:
            print("[WARN] onnxruntime non installé, optimisation ignorée")


def export_to_torchscript(
    model,
    tokenizer,
    output_path: str,
    max_length: int = 128
):
    """
    Exporte un modèle HuggingFace en TorchScript

    Args:
        model: Modèle HuggingFace à exporter
        tokenizer: Tokenizer associé
        output_path: Chemin du fichier TorchScript de sortie (.pt)
        max_length: Longueur max des séquences

    Note:
        TorchScript est recommandé pour déploiement PyTorch natif (C++, mobile)
        Pour DJL Java, préférer ONNX
    """
    model.eval()
    device = next(model.parameters()).device

    # Créer inputs factices
    dummy_text = "Je veux aller de Paris à Lyon"
    inputs = tokenizer(
        dummy_text,
        return_tensors="pt",
        truncation=True,
        max_length=max_length,
        padding="max_length"
    ).to(device)

    # Tracer le modèle
    with torch.no_grad():
        traced_model = torch.jit.trace(
            model,
            (inputs["input_ids"], inputs["attention_mask"])
        )

    # Sauvegarder
    traced_model.save(output_path)
    print(f"[OK] Modèle exporté en TorchScript: {output_path}")


def save_nlu_model_complete(
    model_ner,
    model_intent,
    tokenizer,
    output_dir: str,
    config: Dict,
    export_formats: list = ["pytorch", "onnx"]
):
    """
    Sauvegarde complète d'un système NLU (NER + Intent) avec exports multiples

    Args:
        model_ner: Modèle NER (token classification)
        model_intent: Modèle Intent (sequence classification)
        tokenizer: Tokenizer partagé
        output_dir: Répertoire racine de sortie
        config: Configuration (entity_labels, intent_labels, etc.)
        export_formats: Liste des formats d'export ("pytorch", "onnx", "torchscript")

    Structure de sortie:
        output_dir/
        ├── model_ner/
        │   ├── pytorch_model.bin
        │   ├── config.json
        │   ├── model_ner.onnx (si onnx)
        │   └── labels_config.json
        ├── model_intent/
        │   ├── pytorch_model.bin
        │   ├── config.json
        │   ├── model_intent.onnx (si onnx)
        │   └── labels_config.json
        └── tokenizer/
            ├── tokenizer.json
            └── vocab.txt
    """
    output_dir = Path(output_dir)
    ner_dir = output_dir / "model_ner"
    intent_dir = output_dir / "model_intent"
    tokenizer_dir = output_dir / "tokenizer"

    # PyTorch natif
    if "pytorch" in export_formats:
        save_pytorch_model(
            model_ner,
            tokenizer,
            str(ner_dir),
            {"entity_labels": config.get("entity_labels", [])}
        )
        save_pytorch_model(
            model_intent,
            tokenizer,
            str(intent_dir),
            {"intent_labels": config.get("intent_labels", [])}
        )

        # Tokenizer séparé
        tokenizer.save_pretrained(str(tokenizer_dir))

    # ONNX (pour DJL Java)
    if "onnx" in export_formats:
        export_to_onnx(
            model_ner,
            tokenizer,
            str(ner_dir / "model_ner.onnx"),
            task="ner",
            max_length=config.get("max_length", 128)
        )
        export_to_onnx(
            model_intent,
            tokenizer,
            str(intent_dir / "model_intent.onnx"),
            task="intent",
            max_length=config.get("max_length", 128)
        )

    # TorchScript
    if "torchscript" in export_formats:
        export_to_torchscript(
            model_ner,
            tokenizer,
            str(ner_dir / "model_ner_torchscript.pt"),
            max_length=config.get("max_length", 128)
        )
        export_to_torchscript(
            model_intent,
            tokenizer,
            str(intent_dir / "model_intent_torchscript.pt"),
            max_length=config.get("max_length", 128)
        )

    print(f"\n[OK] Export complet NLU sauvegardé dans {output_dir}")
    print(f"Formats: {', '.join(export_formats)}")


# Exemple d'utilisation DJL Java (documentation)
"""
# DJL Java - Chargement ONNX pour NLU

```java
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslatorContext;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.onnxruntime.engine.OrtEngine;

public class NLUPredictor {

    public static void main(String[] args) throws ModelException, IOException {
        // Charger modèle NER ONNX
        Criteria<String, float[][]> criteriaNER = Criteria.builder()
            .setTypes(String.class, float[][].class)
            .optModelPath(Paths.get("nlu/models/model_ner/model_ner.onnx"))
            .optEngine("OnnxRuntime")
            .optProgress(new ProgressBar())
            .build();

        ZooModel<String, float[][]> modelNER = criteriaNER.loadModel();

        // Charger modèle Intent ONNX
        Criteria<String, float[]> criteriaIntent = Criteria.builder()
            .setTypes(String.class, float[].class)
            .optModelPath(Paths.get("nlu/models/model_intent/model_intent.onnx"))
            .optEngine("OnnxRuntime")
            .optProgress(new ProgressBar())
            .build();

        ZooModel<String, float[]> modelIntent = criteriaIntent.loadModel();

        // Prédiction
        try (Predictor<String, float[][]> predictorNER = modelNER.newPredictor();
             Predictor<String, float[]> predictorIntent = modelIntent.newPredictor()) {

            String text = "Je veux aller de Paris à Lyon";

            // NER
            float[][] nerLogits = predictorNER.predict(text);

            // Intent
            float[] intentLogits = predictorIntent.predict(text);

            System.out.println("Prédiction NER: " + Arrays.toString(nerLogits));
            System.out.println("Prédiction Intent: " + Arrays.toString(intentLogits));
        }
    }
}
```

Dependencies Maven:
```xml
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.25.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.onnxruntime</groupId>
    <artifactId>onnxruntime-engine</artifactId>
    <version>0.25.0</version>
</dependency>
```
"""
