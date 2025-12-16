import json
import sys
from pathlib import Path
from transformers import pipeline

# Ajouter le répertoire parent au path
sys.path.insert(0, str(Path(__file__).parent.parent))

from nlu.gazetteer import CityGazetteer

class HybridNLUPredictor:
    """
    Prédicteur NLU hybride combinant:
    - Modèle NER (apprentissage contextuel)
    - Gazetteer (liste de villes connues)
    - Classification d'intent
    """

    def __init__(self,
                 ner_model_path="nlu/models/model_ner",
                 intent_model_path="nlu/models/model_intent",
                 cities_file="nlu/data/cities.json",
                 use_gazetteer=True):
        """Initialise les pipelines et le gazetteer"""

        # Pipeline NER
        self.ner_pipeline = pipeline(
            "token-classification",
            model=ner_model_path,
            tokenizer=ner_model_path,
            aggregation_strategy="simple"
        )

        # Pipeline Intent
        self.intent_pipeline = pipeline(
            "text-classification",
            model=intent_model_path,
            tokenizer=intent_model_path
        )

        # Gazetteer
        self.use_gazetteer = use_gazetteer
        if use_gazetteer:
            self.gazetteer = CityGazetteer(cities_file)
        else:
            self.gazetteer = None

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
        entities_ner = self._extract_entities_from_ner(ner_result)

        # Si le gazetteer est activé, on l'utilise aussi
        if self.use_gazetteer and self.gazetteer:
            entities_gazetteer = self.gazetteer.extract_entities_with_context(text)

            # Fusionner les résultats (prioriser le gazetteer si plus précis)
            entities = self._merge_entities(entities_ner, entities_gazetteer)
        else:
            entities = entities_ner

        return {
            "text": text,
            "intent": {
                "label": intent_result["label"],
                "confidence": intent_result["score"]
            },
            "entities": entities,
            "sources": {
                "ner": entities_ner,
                "gazetteer": entities_gazetteer if self.use_gazetteer else None
            }
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

    def _merge_entities(self, ner_entities, gazetteer_entities):
        """
        Fusionne les résultats NER et Gazetteer
        Stratégie: Prioriser le gazetteer pour les villes reconnues,
        compléter avec NER si nécessaire
        """
        merged = {"from": "", "to": "", "via": [], "person": ""}

        # Prioriser gazetteer pour FROM et TO (plus précis sur les villes)
        merged["from"] = gazetteer_entities["from"] or ner_entities["from"]
        merged["to"] = gazetteer_entities["to"] or ner_entities["to"]

        # Combiner les VIA (gazetteer + ner)
        via_set = set()
        for via in gazetteer_entities["via"]:
            via_set.add(via)
        for via in ner_entities["via"]:
            via_set.add(via)
        merged["via"] = list(via_set)

        # PERSON vient uniquement du NER (pas dans le gazetteer)
        merged["person"] = ner_entities.get("person", "")

        return merged

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
    # Comparaison avec et sans gazetteer
    print("="*70)
    print("COMPARAISON : NER seul vs NER + Gazetteer")
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

    # Prédicteur avec gazetteer
    predictor_hybrid = HybridNLUPredictor(use_gazetteer=True)

    # Prédicteur sans gazetteer
    predictor_ner_only = HybridNLUPredictor(use_gazetteer=False)

    for text in test_texts:
        print(f"\n{'='*70}")
        print(f"Texte : {text}")
        print(f"{'='*70}")

        # Résultat NER seul
        result_ner = predictor_ner_only.predict(text)
        print(f"\n[NER seul]")
        print(f"  FROM: {result_ner['entities']['from'] or '(vide)'}")
        print(f"  TO: {result_ner['entities']['to'] or '(vide)'}")
        print(f"  VIA: {', '.join(result_ner['entities']['via']) or '(vide)'}")
        print(f"  PERSON: {result_ner['entities'].get('person', '') or '(vide)'}")

        # Résultat hybride
        result_hybrid = predictor_hybrid.predict(text)
        print(f"\n[NER + Gazetteer]")
        print(f"  FROM: {result_hybrid['entities']['from'] or '(vide)'}")
        print(f"  TO: {result_hybrid['entities']['to'] or '(vide)'}")
        print(f"  VIA: {', '.join(result_hybrid['entities']['via']) or '(vide)'}")
        print(f"  PERSON: {result_hybrid['entities'].get('person', '') or '(vide)'}")

        # Comparaison
        if result_ner['entities'] != result_hybrid['entities']:
            print(f"\n  >> Amelioration detectee avec le gazetteer!")
