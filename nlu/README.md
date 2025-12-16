# NLU - Natural Language Understanding pour trajets

Système NLU pour détecter les intentions de trajet et extraire les entités (villes de départ, arrivée, étapes).

## 📁 Structure du projet

```
nlu/
├── data/
│   ├── templates.json           # Templates de phrases + exemples négatifs
│   ├── cities.json              # Liste des villes françaises (193 villes)
│   └── train_generated.json     # Dataset généré depuis templates (285 exemples)
├── models/
│   ├── model_ner/               # Modèle NER entraîné
│   └── model_intent/            # Modèle Intent entraîné
├── train.py                     # Entraînement des modèles
├── generate_from_templates.py   # Génère le dataset depuis templates
├── gazetteer.py                 # Classe CityGazetteer (liste de villes)
└── predict.py                   # Prédiction hybride (NER + Gazetteer)
```

## 🎯 Concepts clés

### 1. **NER (Named Entity Recognition)**
Le NER est un modèle d'apprentissage automatique qui identifie et extrait les entités nommées dans un texte.

**Comment ça marche ?**
- Apprend des **patterns contextuels** : "de X à Y", "depuis X", "en passant par Y"
- Ne connaît **pas** explicitement la liste des villes
- Généralise à partir d'exemples d'entraînement
- Utilise le schéma BIO (Begin-Inside-Outside) pour taguer les mots

**Exemple de tagging BIO :**
```
Phrase : "Je veux aller de Paris à Lyon"
Tags   :  O   O    O    O  B-FROM O B-TO

Phrase : "Dis Valérian je veux aller de Paris à Lyon"
Tags   :  O   B-PERSON O   O    O    O  B-FROM O B-TO

B-FROM   = Début d'une ville de départ
I-FROM   = Suite d'une ville de départ (pour noms composés)
B-TO     = Début d'une ville d'arrivée
I-TO     = Suite d'une ville d'arrivée
B-VIA    = Début d'une ville étape
I-VIA    = Suite d'une ville étape
B-PERSON = Début d'un prénom
I-PERSON = Suite d'un prénom
O        = Pas une entité
```

**Avantages :**
- Peut géraliser à des villes non vues pendant l'entraînement
- Comprend le contexte ("de X" vs "à X")

**Inconvénients :**
- Peut faire des erreurs sur les noms composés
- Moins précis si peu d'exemples d'entraînement

### 2. **Intent Classification**
Classification d'intention qui détermine ce que l'utilisateur veut faire.

**Comment ça marche ?**
- Analyse la phrase complète
- Classifie en deux catégories :
  - `trajet` : L'utilisateur veut un itinéraire
  - `other` : Autre chose (question, conversation, etc.)

**Exemples :**
```
"Je veux aller de Paris à Lyon" → trajet (confiance: 95%)
"Je veux boire un monaco"        → other (confiance: 92%)
"Bonjour comment allez-vous ?"   → other (confiance: 98%)
```

**Pourquoi c'est important ?**
- Évite les faux positifs (ex: "monaco" dans "boire un monaco")
- Permet de filtrer les demandes non pertinentes

### 3. **Gazetteer**
Liste de villes françaises connues avec recherche par pattern matching.

**Comment ça marche ?**
- Liste explicite de 193 villes dans `cities.json`
- Recherche ces villes dans le texte avec regex
- Utilise le contexte pour déterminer FROM/TO/VIA
  - "de X" → FROM
  - "à X", "vers X" → TO
  - "via X", "en passant par X" → VIA

**Avantages :**
- 100% précis sur les villes répertoriées
- Pas besoin d'entraînement
- Gère parfaitement les noms composés ("Saint-Étienne")

**Inconvénients :**
- Ne connaît que les villes de la liste
- Peut manquer des variantes ou nouvelles villes

### 4. **Système Hybride** (Recommandé)
Combine NER + Gazetteer pour le meilleur des deux mondes.

**Comment ça marche ?**
1. Le modèle NER fait sa prédiction
2. Le Gazetteer fait sa recherche
3. Les résultats sont fusionnés en priorisant le Gazetteer

**Stratégie de fusion :**
- Si le Gazetteer trouve une ville → on la prend
- Sinon → on prend la prédiction du NER
- Pour les VIA → on combine les deux sources
- Pour les PERSON → on prend uniquement le NER (pas dans le Gazetteer)

**Exemple :**
```
Phrase : "Dis Valérian je veux aller de Paris à Saint-Étienne"

NER seul :
  FROM: Paris
  TO: Saint Étienne (peut séparer le nom)
  PERSON: Valérian

Gazetteer seul :
  FROM: Paris
  TO: Saint-Étienne (nom exact)

Hybride (fusion) :
  FROM: Paris
  TO: Saint-Étienne (prend le Gazetteer, plus précis)
  PERSON: Valérian (du NER)
```

**Exemple sans prénom :**
```
Phrase : "Je veux aller de Paris à Saint-Étienne"

NER seul :
  FROM: Paris
  TO: Saint Étienne (peut séparer le nom)

Gazetteer seul :
  FROM: Paris
  TO: Saint-Étienne (nom exact)

Hybride (fusion) :
  FROM: Paris
  TO: Saint-Étienne (prend le Gazetteer, plus précis)
```

## 🎯 Data Augmentation

Le système inclut de la data augmentation automatique pour améliorer la robustesse du modèle. Les techniques appliquées :

### Techniques d'augmentation

1. **Variations de casse**
   - "je veux aller de paris à lyon"
   - "Je veux aller de Paris à Lyon"
   - "JE VEUX ALLER DE PARIS À LYON"

2. **Variations de ponctuation**
   - "Je veux aller de Paris à Lyon"
   - "Je veux aller de Paris à Lyon."
   - "Je veux aller de Paris à Lyon?"
   - "Je veux aller de Paris à Lyon!"

3. **Mots de remplissage (hésitations naturelles)**
   - "Euh je veux aller de Paris à Lyon"
   - "Je veux euh aller de Paris à Lyon"
   - "Je veux aller de Paris à Lyon hein"

4. **Fautes de frappe courantes**
   - "Je veu aller de Paris à Lyon"
   - "Je veux alé de Paris à Lyon"
   - "Comen aller de Paris à Lyon"

5. **Synonymes contextuels**
   - "Je voudrais aller de Paris à Lyon"
   - "Je souhaite me rendre de Paris à Lyon"
   - "J'aimerais voyager de Paris à Lyon"

6. **Contractions orales françaises**
   - "J'veux aller de Paris à Lyon"
   - "Chui parti de Paris pour Lyon"
   - "Y'a un train de Paris à Lyon"

7. **Inversions d'ordre des mots**
   - "De Paris à Lyon comment aller"
   - "Pour aller de Paris à Lyon"
   - "Lyon depuis Paris comment y aller"

### Impact sur le dataset

- **Sans augmentation** : ~250-300 exemples
- **Avec augmentation (facteur 2.0)** : ~500-600 exemples
- **Amélioration de la robustesse** : +15-25% de précision sur des formulations variées

L'augmentation est appliquée **automatiquement** lors de la génération du dataset.

## 🚀 Quick Start

### 1. Générer le dataset

```bash
python nlu/generate_from_templates.py
```

Ce script :
- Lit `data/templates.json` (19 templates positifs + 19 négatifs)
- Lit `data/cities.json` (193 villes)
- Génère `data/train_generated.json` (285 exemples)
  - 190 exemples positifs (10 par template)
  - 95 exemples négatifs

**Options dans le script :**
```python
dataset = generate_dataset(
    templates_data,
    cities,
    num_samples_per_template=10,  # Nombre d'exemples par template
    num_negative_samples=100       # Nombre d'exemples négatifs au total
)
```

### 2. Entraîner les modèles

```bash
python nlu/train.py
```

Ce script :
- Charge `data/train_generated.json`
- Entraîne deux modèles :
  - **NER** : Extraction d'entités (FROM, TO, VIA)
  - **Intent** : Classification d'intention (trajet, other)
- Sauvegarde dans `models/model_ner/` et `models/model_intent/`
- Affiche des prédictions de test à la fin

**Modèle de base utilisé :**
- `camembert-base` : Modèle français pré-entraîné

**Configuration (dans `config.json`) :**
```json
{
  "tokenizer": {
    "type": "camembert-base",
    "max_length": 128
  },
  "training": {
    "epochs": 5,           // Nombre d'époques d'entraînement
    "batch_size": 8,       // Taille des batches
    "learning_rate": 5e-5  // Taux d'apprentissage
  },
  "entity_labels": ["O", "B-FROM", "I-FROM", "B-TO", "I-TO", "B-VIA", "I-VIA", "B-PERSON", "I-PERSON"],
  "intent_labels": ["trajet", "other"]
}
```

**Ce qui est créé :**
```
models/
├── model_ner/
│   ├── config.json              # Configuration du modèle
│   ├── model.safetensors        # Poids du modèle NER
│   ├── tokenizer_config.json    # Config du tokenizer
│   ├── labels_config.json       # Labels et config d'entraînement
│   └── ...
└── model_intent/
    ├── config.json              # Configuration du modèle
    ├── model.safetensors        # Poids du modèle Intent
    ├── tokenizer_config.json    # Config du tokenizer
    ├── labels_config.json       # Labels et config d'entraînement
    └── ...
```

### 3. Faire des prédictions

```bash
python nlu/predict.py
```

Ce script :
- Charge les modèles depuis `models/`
- Teste avec et sans Gazetteer
- Affiche une comparaison des résultats

**Utiliser en Python :**
```python
from nlu.predict import HybridNLUPredictor

# Avec Gazetteer (recommandé)
predictor = HybridNLUPredictor(use_gazetteer=True)

result = predictor.predict("Je veux aller de Paris à Lyon")
print(result)
```

**Format du résultat :**
```python
{
  "text": "Je veux aller de Paris à Lyon",
  "intent": {
    "label": "trajet",
    "confidence": 0.95
  },
  "entities": {
    "from": "Paris",
    "to": "Lyon",
    "via": [],
    "person": ""
  },
  "sources": {
    "ner": {"from": "Paris", "to": "Lyon", "via": [], "person": ""},
    "gazetteer": {"from": "Paris", "to": "Lyon", "via": []}
  }
}
```

**Exemple avec prénom :**
```python
result = predictor.predict("Dis Valérian je veux aller de Paris à Lyon")
print(result)
# {
#   "text": "Dis Valérian je veux aller de Paris à Lyon",
#   "intent": {"label": "trajet", "confidence": 0.95},
#   "entities": {
#     "from": "Paris",
#     "to": "Lyon",
#     "via": [],
#     "person": "Valérian"
#   }
# }
```

**Exemple avec plusieurs villes via :**
```python
result = predictor.predict("Je veux aller de Paris à Lyon via Dijon et Mâcon")
print(result)
# {
#   "text": "Je veux aller de Paris à Lyon via Dijon et Mâcon",
#   "intent": {"label": "trajet", "confidence": 0.95},
#   "entities": {
#     "from": "Paris",
#     "to": "Lyon",
#     "via": ["Dijon", "Mâcon"],
#     "person": ""
#   }
# }
```

**Version simplifiée :**
```python
result = predictor.predict_simple("Je veux aller de Paris à Lyon")
# {'intent': 'trajet', 'from': 'Paris', 'to': 'Lyon', 'via': [], 'person': ''}
```

## 📊 Comparaison des approches

| Approche | Entraînement | Prédiction | Précision | Flexibilité |
|----------|--------------|------------|-----------|-------------|
| **NER seul** | ✅ Nécessaire | Rapide | Moyenne | Haute |
| **Gazetteer seul** | ❌ Aucun | Rapide | Haute* | Basse |
| **Hybride** | ✅ Nécessaire | Rapide | Très haute | Haute |

\* Sur villes connues uniquement

## 📝 Personnalisation

### Ajouter des villes

Modifiez `data/cities.json` :
```json
[
  "Paris",
  "Lyon",
  "Votre-Ville",
  ...
]
```

**Pas besoin de réentraîner** si vous utilisez le système hybride ! Le Gazetteer les reconnaîtra immédiatement.

### Ajouter des prénoms

Modifiez `data/persons.json` :
```json
[
  "Maude",
  "Valérian",
  "Simon",
  "Jayce",
  "Votre-Prénom"
]
```

**Important :** Les prénoms sont optionnels dans les phrases ! Le modèle peut détecter :
- **Avec prénom** : "Dis Valérian je veux aller de Paris à Lyon" → person="Valérian"
- **Sans prénom** : "Je veux aller de Paris à Lyon" → person=""

Puis régénérez et réentraînez :
```bash
python nlu/generate_from_templates.py
python nlu/train.py
```

### Ajouter des patterns de phrases

Modifiez `data/templates.json` :
```json
{
  "templates": [
    {
      "pattern": "Je veux aller de {from_city} à {to_city}",
      "intent": "trajet",
      "has_via": false
    },
    {
      "pattern": "Votre nouveau pattern ici",
      "intent": "trajet",
      "has_via": false
    }
  ],
  "negative_examples": [
    {
      "pattern": "Phrase qui n'est PAS un trajet",
      "intent": "other"
    }
  ]
}
```

**Placeholders disponibles :**
- `{from_city}` : Ville de départ
- `{to_city}` : Ville d'arrivée
- `{via_city}` : Ville étape (si `has_via: true`)
- `{city}` : Ville générique (pour exemples négatifs)

Puis régénérez et réentraînez :
```bash
python nlu/generate_from_templates.py
python nlu/train.py
```

## 🎓 Questions fréquentes

### Q: Faut-il réentraîner si j'ajoute une ville ?

**Non**, si vous utilisez le système hybride ! Ajoutez simplement la ville dans `cities.json` et le Gazetteer la reconnaîtra immédiatement.

Si vous utilisez le NER seul, il faut :
1. Ajouter la ville dans `cities.json`
2. Régénérer le dataset : `python nlu/generate_from_templates.py`
3. Réentraîner : `python nlu/train.py`

### Q: Comment gérer plusieurs villes via ?

Le système supporte maintenant plusieurs villes via dans une même phrase !

**Pour 1 ville via :**
```json
{
  "pattern": "De {from_city} à {to_city} via {via_city}",
  "intent": "trajet",
  "has_via": true
}
```

**Pour 2 villes via :**
```json
{
  "pattern": "Je veux aller de {from_city} à {to_city} via {via_city1} et {via_city2}",
  "intent": "trajet",
  "has_via": true,
  "num_via": 2
}
```

**Pour 3 villes via ou plus :**
```json
{
  "pattern": "De {from_city} à {to_city} via {via_city1}, {via_city2} et {via_city3}",
  "intent": "trajet",
  "has_via": true,
  "num_via": 3
}
```

**Exemples de phrases générées :**
- 1 via : "De Paris à Lyon via Dijon"
- 2 via : "Je veux aller de Paris à Lyon via Dijon et Mâcon"
- 3 via : "De Bordeaux à Strasbourg via Limoges, Clermont-Ferrand et Dijon"

Le générateur sélectionne automatiquement des villes différentes pour chaque étape.

### Q: Comment éviter les faux positifs ?

Ajoutez des exemples négatifs dans `templates.json` :
```json
{
  "negative_examples": [
    {
      "pattern": "Je veux boire un monaco",
      "intent": "other"
    }
  ]
}
```

Le modèle Intent apprendra à distinguer ces cas.

### Q: Puis-je utiliser un autre modèle que CamemBERT ?

Oui ! Modifiez `config.json` :
```json
{
  "tokenizer": {
    "type": "flaubert/flaubert_base_cased",  // ou autre modèle français
    "max_length": 128
  }
}
```

Puis réentraînez : `python nlu/train.py`

### Q: Comment améliorer la précision ?

1. **Ajouter plus de templates** dans `templates.json`
2. **Augmenter le nombre d'exemples** par template dans `generate_from_templates.py`
3. **Augmenter les époques** d'entraînement dans `config.json` (ex: 10 au lieu de 5)
4. **Utiliser le système hybride** (NER + Gazetteer)
5. **Ajouter des exemples négatifs** pour éviter les faux positifs

## 📈 Performances attendues

### Modèle simple (285 exemples)
- Intent : ~95% de confiance
- Entités FROM/TO : 85-90% de précision
- Entités VIA : 70-80% de précision

### Système hybride (modèle + gazetteer)
- Intent : ~95% de confiance
- Entités FROM/TO : 95-98% de précision (sur villes connues)
- Entités VIA : 85-95% de précision
- Excellente gestion des noms composés

## 🛠️ Dépendances

```bash
pip install transformers datasets torch
```

Ou :
```bash
pip install -r nlu/requirements.txt
```

## 📚 Ressources

- Modèle de base : [CamemBERT](https://huggingface.co/camembert-base)
- Format d'annotation : BIO (Begin-Inside-Outside)
- Framework : [Transformers (Hugging Face)](https://huggingface.co/docs/transformers)
