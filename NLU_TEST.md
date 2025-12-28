# Test NLU avec DJL et MLflow

Ce document explique comment tester le chargement et l'inférence des modèles NLU depuis MLflow.

## Architecture

- **NLU Module** : Contient l'interface `INLUService` et l'implémentation `NLUPredictService` avec les Translators
- **Orchestrator Module** : Expose l'endpoint REST via `NLUTestController`
- **Common Module** : Fournit `ModelLoader` pour charger les modèles depuis MLflow

## Démarrage

Au lancement de l'application, les modèles NLU sont automatiquement chargés depuis MLflow :
1. Le modèle Intent Classification (`nlu-intent`)
2. Le modèle NER (`nlu-ner`)

Les logs montreront le processus de téléchargement et de chargement des modèles.

## Endpoints disponibles

### 1. Health Check
```bash
GET http://localhost:8080/api/nlu/health
```

### 2. Analyse NLU complète (Intent + Entities)
```bash
POST http://localhost:8080/api/nlu/predict
Content-Type: application/json

{
  "text": "Je veux réserver un train pour Paris demain"
}
```

**Réponse** :
```json
{
  "intent": {
    "intent": "book_train",
    "confidence": 0.95
  },
  "entities": {
    "entities": [
      {
        "text": "Paris",
        "label": "LOC",
        "confidence": 0.95,
        "start": 33,
        "end": 38
      },
      {
        "text": "demain",
        "label": "DATE",
        "confidence": 0.92,
        "start": 39,
        "end": 45
      }
    ]
  }
}
```

## Configuration MLflow

Les modèles sont chargés depuis : `https://mlflow.bryan-ferrando.fr`

Variables d'environnement disponibles :
- `MLFLOW_TRACKING_URI` : URL du serveur MLflow (défaut: https://mlflow.bryan-ferrando.fr)
- `MLFLOW_MODEL_STAGE` : Alias du modèle (défaut: latest)

## Modèles requis dans MLflow

Les modèles doivent être enregistrés dans MLflow avec les noms suivants :
- `nlu-intent` : Modèle de classification d'intent
- `nlu-ner` : Modèle d'extraction d'entités nommées

Chaque modèle doit avoir un alias `latest` (ou configurer `MLFLOW_MODEL_STAGE`).

## TODO : Implémenter les Translators

Les fichiers `IntentTranslator.java` et `NERTranslator.java` contiennent actuellement du code placeholder.

Vous devez :
1. Implémenter la tokenization dans `processInput()`
2. Implémenter le décodage des sorties dans `processOutput()`
3. Adapter les labels aux classes de vos modèles

## Exemple de test avec curl

```bash
# 1. Démarrer l'application (les modèles se chargent automatiquement)
# Vérifier les logs pour voir le téléchargement des modèles depuis MLflow

# 2. Vérifier que le service fonctionne
curl http://localhost:8080/api/nlu/health

# 3. Tester l'analyse NLU complète
curl -X POST http://localhost:8080/api/nlu/predict \
  -H "Content-Type: application/json" \
  -d '{"text": "Je veux réserver un train pour Paris demain"}'
```

## Logs

Les logs montreront :
- Les engines DJL disponibles (PyTorch, ONNX Runtime)
- Le téléchargement des modèles depuis MLflow
- Le chargement des modèles en mémoire
- Les prédictions effectuées

Regardez les logs de l'application pour voir le processus de chargement des modèles.
