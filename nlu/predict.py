import json
import sys
from pathlib import Path
from transformers import pipeline
import torch

# Ajouter le répertoire parent au path
sys.path.insert(0, str(Path(__file__).parent.parent))

# Charger les cas de test depuis un JSON
predict_file = Path("nlu/data/predict.json")
if not predict_file.exists():
    raise FileNotFoundError(f"Fichier de test non trouvé: {predict_file}")

with open(predict_file, "r", encoding="utf-8") as f:
    test_cases = json.load(f)  # Chaque élément doit avoir "text", "intent", "from", "to", "via", "person"


class NLUPredictor:
    """
    Prédicteur NLU combinant:
    - Modèle NER (apprentissage contextuel)
    - Classification d'intent
    """
    def __init__(self,
                 ner_model_path="nlu/models/model_ner",
                 intent_model_path="nlu/models/model_intent"):
        """Initialise les pipelines"""
        device = 0 if torch.cuda.is_available() else -1

        self.ner_pipeline = pipeline(
            "token-classification",
            model=ner_model_path,
            tokenizer=ner_model_path,
            aggregation_strategy="simple",
            device=device
        )

        self.intent_pipeline = pipeline(
            "text-classification",
            model=intent_model_path,
            tokenizer=intent_model_path,
            device=device
        )

        config_path = Path(ner_model_path) / "labels_config.json"
        if config_path.exists():
            with open(config_path) as f:
                self.config = json.load(f)
        else:
            self.config = None

    def predict(self, text):
        intent_result = self.intent_pipeline(text)[0]
        ner_result = self.ner_pipeline(text)
        entities = self._extract_entities_from_ner(ner_result)

        return {
            "text": text,
            "intent": {
                "label": intent_result["label"],
                "confidence": intent_result["score"]
            },
            "entities": entities
        }

    def _extract_entities_from_ner(self, ner_result):
        entities = {"from": "", "to": "", "via": [], "person": ""}

        for r in ner_result:
            entity_type = r.get("entity_group", r.get("entity")).replace("B-", "").replace("I-", "")
            word = r["word"].replace("▁", " ").strip()

            if entity_type == "FROM":
                entities["from"] += (" " + word if entities["from"] else word)
            elif entity_type == "TO":
                entities["to"] += (" " + word if entities["to"] else word)
            elif entity_type == "VIA":
                if not entities["via"] or word not in entities["via"][-1]:
                    entities["via"].append(word)
                else:
                    entities["via"][-1] += " " + word
            elif entity_type == "PERSON":
                entities["person"] += (" " + word if entities["person"] else word)

        entities["from"] = entities["from"].strip()
        entities["to"] = entities["to"].strip()
        entities["via"] = [v.strip() for v in entities["via"]]
        entities["person"] = entities["person"].strip()

        return entities

    def batch_predict(self, texts):
        return [self.predict(text) for text in texts]

    def predict_simple(self, text):
        result = self.predict(text)
        return {
            "intent": result["intent"]["label"],
            "from": result["entities"]["from"],
            "to": result["entities"]["to"],
            "via": result["entities"]["via"]
        }


if __name__ == "__main__":
    print("="*70)
    print("TEST DU PREDICTEUR NLU")
    print("="*70)

    predictor = NLUPredictor()
    total = len(test_cases)
    correct_intent = 0
    correct_from = 0
    correct_to = 0
    correct_via = 0
    correct_person = 0
    fully_correct = 0
    results = []

    for i, test_case in enumerate(test_cases, 1):
        text = test_case["text"]
        expected = test_case

        result = predictor.predict(text)
        predicted = {
            "intent": result["intent"]["label"],
            "from": result["entities"]["from"].lower() if result["entities"]["from"] else "",
            "to": result["entities"]["to"].lower() if result["entities"]["to"] else "",
            "via": [v.lower() for v in result["entities"]["via"]],
            "person": result["entities"].get("person", "")
        }

        expected_norm = {
            "intent": expected["intent"],
            "from": expected["from"].lower() if expected["from"] else "",
            "to": expected["to"].lower() if expected["to"] else "",
            "via": [v.lower() for v in expected["via"]],
            "person": expected["person"]
        }

        is_intent_correct = predicted["intent"] == expected_norm["intent"]
        is_from_correct = predicted["from"] == expected_norm["from"]
        is_to_correct = predicted["to"] == expected_norm["to"]
        is_via_correct = predicted["via"] == expected_norm["via"]
        is_person_correct = predicted["person"] == expected_norm["person"]
        is_fully_correct = all([is_intent_correct, is_from_correct, is_to_correct, is_via_correct, is_person_correct])

        if is_intent_correct: correct_intent += 1
        if is_from_correct: correct_from += 1
        if is_to_correct: correct_to += 1
        if is_via_correct: correct_via += 1
        if is_person_correct: correct_person += 1
        if is_fully_correct: fully_correct += 1

        results.append({
            "text": text,
            "correct": is_fully_correct,
            "errors": {
                "intent": not is_intent_correct,
                "from": not is_from_correct,
                "to": not is_to_correct,
                "via": not is_via_correct,
                "person": not is_person_correct
            }
        })

    # Résumé final
    print(f"\n{'='*70}")
    print("RÉSUMÉ DES RÉSULTATS")
    print(f"{'='*70}\n")
    print(f"Tests réussis: {fully_correct}/{total} ({fully_correct/total*100:.1f}%)")
    print("\nDétail par composant:")
    print(f"  Intent:  {correct_intent}/{total} ({correct_intent/total*100:.1f}%)")
    print(f"  FROM:    {correct_from}/{total} ({correct_from/total*100:.1f}%)")
    print(f"  TO:      {correct_to}/{total} ({correct_to/total*100:.1f}%)")
    print(f"  VIA:     {correct_via}/{total} ({correct_via/total*100:.1f}%)")
    print(f"  PERSON:  {correct_person}/{total} ({correct_person/total*100:.1f}%)")
