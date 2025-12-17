import json
import sys
from pathlib import Path
from transformers import pipeline
import torch

# Ajouter le répertoire parent au path
sys.path.insert(0, str(Path(__file__).parent.parent))

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

        # Détection GPU automatique (0 = GPU, -1 = CPU)
        device = 0 if torch.cuda.is_available() else -1

        # Pipeline NER
        self.ner_pipeline = pipeline(
            "token-classification",
            model=ner_model_path,
            tokenizer=ner_model_path,
            aggregation_strategy="simple",
            device=device
        )

        # Pipeline Intent
        self.intent_pipeline = pipeline(
            "text-classification",
            model=intent_model_path,
            tokenizer=intent_model_path,
            device=device
        )

        # Charger la config
        config_path = Path(ner_model_path) / "labels_config.json"
        if config_path.exists():
            with open(config_path) as f:
                self.config = json.load(f)
        else:
            self.config = None

    def predict(self, text):
        """
        Prédit l'intent et les entités d'un texte

        Args:
            text (str): Le texte à analyser

        Returns:
            dict: Résultat avec l'intent et les entités
        """
        # Prédire l'intent
        intent_result = self.intent_pipeline(text)[0]

        # Extraire les entités avec le modèle NER
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
        """Extrait les entités du résultat NER"""
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

        # Nettoyer les espaces
        entities["from"] = entities["from"].strip()
        entities["to"] = entities["to"].strip()
        entities["via"] = [v.strip() for v in entities["via"]]
        entities["person"] = entities["person"].strip()

        return entities

    def batch_predict(self, texts):
        """Prédit plusieurs textes en batch"""
        return [self.predict(text) for text in texts]

    def predict_simple(self, text):
        """Version simplifiée qui retourne seulement les entités principales"""
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

    test_texts = [
        "Je veux aller de Paris à Lyon",
        "Je veux aller de Marseille à Nice en passant par Cannes",
        "Comment me rendre à Port-Boulet depuis la gare de Tours ?",
        "Je veux faire Saint Denis Saint Etienne",
        "Aller de Lille à Strasbourg en passant par Reims",
        "Voyage de Bordeaux à La Rochelle via Rochefort",
        "Je pars de Nantes pour aller à Montaigu",
        "Dis Valérian je veux aller de Paris à Lyon",
        "Ok Simon aller de Marseille à Nice",
        "Hey Maude comment aller de Lille à Strasbourg",
        "Jayce je voudrais aller de Bordeaux à Toulouse",
        "Je veux aller de Paris à Lyon via Dijon et Mâcon",
        "Aller de Marseille à Lille en passant par Lyon et Dijon",
        "De Bordeaux à Strasbourg via Limoges, Clermont-Ferrand et Dijon"
    ]

    # Prédicteur NLU
    predictor = NLUPredictor()

    for text in test_texts:
        print(f"\n{'='*70}")
        print(f"Texte : {text}")
        print(f"{'='*70}")

        # Résultat
        result = predictor.predict(text)
        print(f"\n[Intent] {result['intent']['label']} ({result['intent']['confidence']:.2%})")
        print(f"  FROM: {result['entities']['from'] or '(vide)'}")
        print(f"  TO: {result['entities']['to'] or '(vide)'}")
        print(f"  VIA: {', '.join(result['entities']['via']) or '(vide)'}")
        print(f"  PERSON: {result['entities'].get('person', '') or '(vide)'}")
