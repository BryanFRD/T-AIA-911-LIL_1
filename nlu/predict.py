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

    # Test cases avec résultats attendus
    test_cases = [
        # === Cas simples (FROM -> TO) ===
        {"text": "Je veux aller de Paris à Lyon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "Trajet Marseille Nice", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": [], "person": ""},
        {"text": "Comment me rendre à Port-Boulet depuis la gare de Tours ?", "intent": "trajet", "from": "Tours", "to": "Port-Boulet", "via": [], "person": ""},

        # === Sans FROM (position implicite) ===
        {"text": "Emmène-moi à Lyon", "intent": "trajet", "from": "", "to": "Lyon", "via": [], "person": ""},
        {"text": "Je veux aller à Bordeaux", "intent": "trajet", "from": "", "to": "Bordeaux", "via": [], "person": ""},
        {"text": "Direction Marseille", "intent": "trajet", "from": "", "to": "Marseille", "via": [], "person": ""},
        {"text": "Comment aller à Strasbourg", "intent": "trajet", "from": "", "to": "Strasbourg", "via": [], "person": ""},

        # === Avec VIA (1-3 villes) ===
        {"text": "Je veux aller de Marseille à Nice en passant par Cannes", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": ["Cannes"], "person": ""},
        {"text": "Aller de Lille à Strasbourg en passant par Reims", "intent": "trajet", "from": "Lille", "to": "Strasbourg", "via": ["Reims"], "person": ""},
        {"text": "De Bordeaux à Strasbourg via Limoges, Clermont-Ferrand et Dijon", "intent": "trajet", "from": "Bordeaux", "to": "Strasbourg", "via": ["Limoges", "Clermont-Ferrand", "Dijon"], "person": ""},
        {"text": "Route Paris Lyon via Dijon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": ["Dijon"], "person": ""},

        # === Avec interpellations de personnes ===
        {"text": "Hey Maude je veux aller de Paris à Lyon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": "Maude"},
        {"text": "Dis Valérian, emmène-moi à Marseille", "intent": "trajet", "from": "", "to": "Marseille", "via": [], "person": "Valérian"},
        {"text": "Ok Simon aller de Marseille à Nice", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": [], "person": "Simon"},
        {"text": "Écoute Jayce, comment aller à Bordeaux", "intent": "trajet", "from": "", "to": "Bordeaux", "via": [], "person": "Jayce"},
        {"text": "Yo Maude trajet Paris Lyon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": "Maude"},

        # === Langage oral / SMS ===
        {"text": "jveux alé de paris a lyon", "intent": "trajet", "from": "paris", "to": "lyon", "via": [], "person": ""},
        {"text": "cmt aller a marseille", "intent": "trajet", "from": "", "to": "marseille", "via": [], "person": ""},
        {"text": "trajet pr bordeaux", "intent": "trajet", "from": "", "to": "bordeaux", "via": [], "person": ""},

        # === Avec fautes de frappe ===
        {"text": "Je vuex aller de Pars à Lyin", "intent": "trajet", "from": "Pars", "to": "Lyin", "via": [], "person": ""},
        {"text": "Trahjet Marseil Nice", "intent": "trajet", "from": "Marseil", "to": "Nice", "via": [], "person": ""},

        # === Avec contractions orales ===
        {"text": "chuis à Paris, j'veux aller à Lyon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "j'dois m'rendre de Marseille à Nice", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": [], "person": ""},

        # === Avec mots parasites ===
        {"text": "Genre je veux aller de Paris à Lyon tu vois", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "Bon euh je dois aller à Marseille enfin", "intent": "trajet", "from": "", "to": "Marseille", "via": [], "person": ""},
        {"text": "Alors voilà je voudrais aller de Lille à Strasbourg", "intent": "trajet", "from": "Lille", "to": "Strasbourg", "via": [], "person": ""},

        # === Avec répétitions / bégaiement ===
        {"text": "Je je veux aller de Paris à Lyon", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "tra- trajet Marseille Nice", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": [], "person": ""},

        # === Ordre des mots inversé ===
        {"text": "De Paris à Lyon je veux aller", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "Aller à Marseille je dois", "intent": "trajet", "from": "", "to": "Marseille", "via": [], "person": ""},

        # === Cas complexes (plusieurs difficultés combinées) ===
        {"text": "Hey Maude euh genre jveux alé d paris a lyon tu vois", "intent": "trajet", "from": "paris", "to": "lyon", "via": [], "person": "Maude"},
        {"text": "Dis Simon, chuis à Bordeaux, j'dois m'rendre à Toulouse via Agen", "intent": "trajet", "from": "Bordeaux", "to": "Toulouse", "via": ["Agen"], "person": "Simon"},
        {"text": "Yo Valérian trajet pr marseille stp", "intent": "trajet", "from": "", "to": "marseille", "via": [], "person": "Valérian"},
        {"text": "Écoute Jayce euh... je je veux aller de Lille à Strasbourg en passant par Reims et Metz", "intent": "trajet", "from": "Lille", "to": "Strasbourg", "via": ["Reims", "Metz"], "person": "Jayce"},

        # === Lowercase / UPPERCASE / MiXeD ===
        {"text": "je veux aller de paris à lyon", "intent": "trajet", "from": "paris", "to": "lyon", "via": [], "person": ""},
        {"text": "JE VEUX ALLER DE PARIS À LYON", "intent": "trajet", "from": "PARIS", "to": "LYON", "via": [], "person": ""},
        {"text": "Je VeUx AlLeR dE pArIs À lYoN", "intent": "trajet", "from": "pArIs", "to": "lYoN", "via": [], "person": ""},

        # === Ponctuation variée ===
        {"text": "Je veux aller de Paris à Lyon!", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "Trajet Paris Lyon...", "intent": "trajet", "from": "Paris", "to": "Lyon", "via": [], "person": ""},
        {"text": "Comment aller de Marseille à Nice?", "intent": "trajet", "from": "Marseille", "to": "Nice", "via": [], "person": ""},

        # === Exemples négatifs (pas de trajet) ===
        {"text": "Quelle heure est-il", "intent": "other", "from": "", "to": "", "via": [], "person": ""},
        {"text": "J'adore Paris", "intent": "other", "from": "", "to": "", "via": [], "person": ""},
        {"text": "Mon frère habite à Lyon", "intent": "other", "from": "", "to": "", "via": [], "person": ""},
        {"text": "Je veux boire un café", "intent": "other", "from": "", "to": "", "via": [], "person": ""},
    ]

    # Prédicteur NLU
    predictor = NLUPredictor()

    # Statistiques
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

        # Résultat
        result = predictor.predict(text)
        predicted = {
            "intent": result["intent"]["label"],
            "from": result["entities"]["from"].lower() if result["entities"]["from"] else "",
            "to": result["entities"]["to"].lower() if result["entities"]["to"] else "",
            "via": [v.lower() for v in result["entities"]["via"]],
            "person": result["entities"].get("person", "")
        }

        # Normaliser les attendus pour comparaison (case-insensitive)
        expected_norm = {
            "intent": expected["intent"],
            "from": expected["from"].lower() if expected["from"] else "",
            "to": expected["to"].lower() if expected["to"] else "",
            "via": [v.lower() for v in expected["via"]],
            "person": expected["person"]
        }

        # Vérification
        is_intent_correct = predicted["intent"] == expected_norm["intent"]
        is_from_correct = predicted["from"] == expected_norm["from"]
        is_to_correct = predicted["to"] == expected_norm["to"]
        is_via_correct = predicted["via"] == expected_norm["via"]
        is_person_correct = predicted["person"] == expected_norm["person"]
        is_fully_correct = all([is_intent_correct, is_from_correct, is_to_correct, is_via_correct, is_person_correct])

        if is_intent_correct:
            correct_intent += 1
        if is_from_correct:
            correct_from += 1
        if is_to_correct:
            correct_to += 1
        if is_via_correct:
            correct_via += 1
        if is_person_correct:
            correct_person += 1
        if is_fully_correct:
            fully_correct += 1

        # Affichage détaillé
        print(f"\n{'='*70}")
        print(f"Test {i}/{total}: {text}")
        print(f"{'='*70}")
        print(f"\n[Intent] {result['intent']['label']} ({result['intent']['confidence']:.2%}) {'✅' if is_intent_correct else '❌ (attendu: ' + expected['intent'] + ')'}")
        print(f"  FROM: {result['entities']['from'] or '(vide)'} {'✅' if is_from_correct else '❌ (attendu: ' + (expected['from'] or '(vide)') + ')'}")
        print(f"  TO: {result['entities']['to'] or '(vide)'} {'✅' if is_to_correct else '❌ (attendu: ' + (expected['to'] or '(vide)') + ')'}")
        print(f"  VIA: {', '.join(result['entities']['via']) or '(vide)'} {'✅' if is_via_correct else '❌ (attendu: ' + (', '.join(expected['via']) or '(vide)') + ')'}")
        print(f"  PERSON: {result['entities'].get('person', '') or '(vide)'} {'✅' if is_person_correct else '❌ (attendu: ' + (expected['person'] or '(vide)') + ')'}")

        if is_fully_correct:
            print("  ✅ SUCCÈS COMPLET")
        else:
            print("  ❌ ERREUR(S) DÉTECTÉE(S)")

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

    # Tests échoués
    failed_tests = [r for r in results if not r["correct"]]
    if failed_tests:
        print(f"\n{'='*70}")
        print(f"TESTS ÉCHOUÉS ({len(failed_tests)})")
        print(f"{'='*70}\n")
        for i, failed in enumerate(failed_tests, 1):
            error_types = [k for k, v in failed["errors"].items() if v]
            print(f"{i}. {failed['text']}")
            print(f"   Erreurs: {', '.join(error_types)}")

    print(f"\n{'='*70}")
