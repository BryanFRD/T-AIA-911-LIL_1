"""
Module de sauvegarde et export de modèles pour production

Supporte:
- PyTorch natif (.pt, .pth)
- TorchScript (.pt) - pour production PyTorch et DJL Java
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


def export_to_torchscript(
    model,
    tokenizer,
    output_path: str,
    max_length: int = 128
):
    """
    Exporte un modèle HuggingFace en TorchScript pour production

    Args:
        model: Modèle HuggingFace à exporter
        tokenizer: Tokenizer associé
        output_path: Chemin du fichier TorchScript de sortie (.pt)
        max_length: Longueur max des séquences

    Note:
        TorchScript est utilisé pour déploiement PyTorch natif (C++, mobile) et DJL Java
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


def export_simple_model_to_torchscript(
    model,
    output_path: str,
    input_shape: tuple = (1, 10)
):
    """
    Exporte un modèle PyTorch simple en TorchScript (sans tokenizer)

    Args:
        model: Modèle PyTorch à exporter
        output_path: Chemin du fichier TorchScript de sortie (.pt)
        input_shape: Shape de l'input factice pour le tracing

    Note:
        Pour modèles simples (STT/TTS dummy) sans tokenizer
    """
    model.eval()
    device = next(model.parameters()).device

    # Créer input factice
    dummy_input = torch.randn(*input_shape).to(device)

    # Tracer le modèle
    with torch.no_grad():
        traced_model = torch.jit.trace(model, dummy_input)

    # Sauvegarder
    traced_model.save(output_path)
    print(f"[OK] Modèle exporté en TorchScript: {output_path}")


def save_nlu_model_complete(
    model_ner,
    model_intent,
    tokenizer,
    output_dir: str,
    config: Dict,
    export_torchscript: bool = True
):
    """
    Sauvegarde complète d'un système NLU (NER + Intent)

    Args:
        model_ner: Modèle NER (token classification)
        model_intent: Modèle Intent (sequence classification)
        tokenizer: Tokenizer partagé
        output_dir: Répertoire racine de sortie
        config: Configuration (entity_labels, intent_labels, etc.)
        export_torchscript: Exporter également en TorchScript

    Structure de sortie:
        output_dir/
        ├── model_ner/
        │   ├── pytorch_model.bin
        │   ├── config.json
        │   ├── model_ner_jit.pt (si torchscript)
        │   └── labels_config.json
        ├── model_intent/
        │   ├── pytorch_model.bin
        │   ├── config.json
        │   ├── model_intent_jit.pt (si torchscript)
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

    # TorchScript
    if export_torchscript:
        export_to_torchscript(
            model_ner,
            tokenizer,
            str(ner_dir / "model_ner_jit.pt"),
            max_length=config.get("max_length", 128)
        )
        export_to_torchscript(
            model_intent,
            tokenizer,
            str(intent_dir / "model_intent_jit.pt"),
            max_length=config.get("max_length", 128)
        )

    print(f"\n[OK] Export complet NLU sauvegardé dans {output_dir}")
    if export_torchscript:
        print("Formats: PyTorch + TorchScript")


# Exemple d'utilisation DJL Java avec TorchScript (documentation)
"""
# DJL Java - Chargement TorchScript pour NLU

```java
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.ndarray.NDList;
import ai.djl.Model;
import ai.djl.ModelException;

public class NLUPredictor {

    public static void main(String[] args) throws ModelException, IOException {
        // Charger modèle NER TorchScript
        Criteria<NDList, NDList> criteriaNER = Criteria.builder()
            .setTypes(NDList.class, NDList.class)
            .optModelPath(Paths.get("nlu/models/model_ner/model_ner_jit.pt"))
            .optEngine("PyTorch")
            .build();

        ZooModel<NDList, NDList> modelNER = criteriaNER.loadModel();

        // Charger modèle Intent TorchScript
        Criteria<NDList, NDList> criteriaIntent = Criteria.builder()
            .setTypes(NDList.class, NDList.class)
            .optModelPath(Paths.get("nlu/models/model_intent/model_intent_jit.pt"))
            .optEngine("PyTorch")
            .build();

        ZooModel<NDList, NDList> modelIntent = criteriaIntent.loadModel();

        // Prédiction avec DJL
        try (Predictor<NDList, NDList> predictorNER = modelNER.newPredictor();
             Predictor<NDList, NDList> predictorIntent = modelIntent.newPredictor()) {

            // Préparer inputs (tokenization à faire côté Java)
            NDManager manager = NDManager.newBaseManager();
            NDList input = new NDList(inputIds, attentionMask);

            // NER
            NDList nerOutput = predictorNER.predict(input);

            // Intent
            NDList intentOutput = predictorIntent.predict(input);

            System.out.println("NER logits: " + nerOutput.get(0));
            System.out.println("Intent logits: " + intentOutput.get(0));
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
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.25.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-auto</artifactId>
    <version>2.0.1</version>
</dependency>
```
"""
