# Guide CI/CD - GitLab Model Registry

Ce guide explique comment utiliser le pipeline GitLab CI/CD pour entraîner, packager et versionner les modèles IA.

## 🏗️ Architecture du Pipeline

Le pipeline est composé de 5 stages :

```
install → test → train → package → release
```

### Stages

1. **install** : Installation des dépendances Python
2. **test** : Tests de qualité (linting, SonarQube)
3. **train** : Entraînement des modèles (manuel)
4. **package** : Packaging des modèles en archives .tar.gz
5. **release** : Upload vers GitLab Package Registry

## 🎯 Modèles Indépendants

Chaque modèle est géré indépendamment :

- **NLU** (Natural Language Understanding)
- **STT** (Speech-to-Text)
- **TTS** (Text-to-Speech)

Les jobs ne se déclenchent que si les fichiers du modèle concerné ont changé.

## 🚀 Workflow Complet

### 1. Développement sur une branche

```bash
# Créer une branche
git checkout -b feature/improve-nlu

# Modifier le code NLU
vim nlu/data/templates.json

# Commit et push
git add nlu/
git commit -m "feat(nlu): add new templates"
git push origin feature/improve-nlu
```

**Ce qui se passe :**
- ✅ `install:dependencies` s'exécute
- ✅ `test:lint` s'exécute
- ✅ `test:sonarqube` s'exécute
- ⏸️ `train:nlu` disponible en **manuel**

### 2. Entraîner un modèle (manuel)

Dans GitLab CI/CD > Pipelines :

1. Cliquer sur le pipeline de votre branche
2. Cliquer sur le bouton ▶️ "Play" du job `train:nlu`
3. Le job s'exécute et produit les artifacts

**Artifacts produits :**
- `nlu/models/model_ner/`
- `nlu/models/model_intent/`
- `nlu/data/train_generated.json`

### 3. Merger vers main

```bash
# Créer une Merge Request
# Une fois approuvée et mergée vers main

git checkout main
git pull
```

**Ce qui se passe sur main :**
- ✅ `install:dependencies`
- ✅ `test:lint`
- ✅ `test:sonarqube`
- ⏸️ `train:nlu` (manuel)
- ✅ `package:nlu` (si entraînement fait)
- ✅ `release:nlu` (si package créé)

### 4. Créer un tag pour release

```bash
# Créer un tag de version
git tag -a v1.0.0 -m "Release v1.0.0: NLU model improvements"
git push origin v1.0.0
```

**Ce qui se passe sur un tag :**
- ✅ Tous les stages s'exécutent
- ✅ `release:notes` crée une release GitLab
- ✅ Les modèles sont uploadés avec la version `v1.0.0`

## 📦 Versioning des Modèles

### Format de version

Le versioning suit cette logique :

- **Sur un tag** : `v1.0.0` → version = `v1.0.0`
- **Sur main** : commit `abc1234` → version = `abc1234`
- **Sur branche** : commit `def5678` → version = `def5678`

### Nommage des packages

```
nlu-model-{VERSION}.tar.gz
stt-model-{VERSION}.tar.gz
tts-model-{VERSION}.tar.gz
```

### Métadonnées incluses

Chaque package contient un fichier `metadata.json` :

```json
{
  "model_name": "nlu",
  "version": "v1.0.0",
  "commit": "abc123def456",
  "branch": "main",
  "pipeline_id": "12345",
  "created_at": "2025-12-16T10:30:00Z",
  "components": {
    "ner": "model_ner",
    "intent": "model_intent"
  }
}
```

## 📥 Télécharger un Modèle

### Depuis GitLab UI

1. Aller dans **Packages & Registries > Package Registry**
2. Sélectionner le modèle et la version
3. Cliquer sur "Download"

### Avec curl

```bash
# Récupérer votre Personal Access Token depuis GitLab
# Settings > Access Tokens > Create with scope 'read_api'

# Télécharger un modèle
curl --header "PRIVATE-TOKEN: <your_token>" \
     "https://gitlab.com/api/v4/projects/<project_id>/packages/generic/nlu-model/v1.0.0/nlu-model-v1.0.0.tar.gz" \
     --output nlu-model.tar.gz

# Extraire
tar -xzf nlu-model.tar.gz
```

### Avec Python

```python
import requests
import tarfile

def download_model(model_name, version, token, project_id):
    """Télécharge un modèle depuis GitLab Package Registry"""
    url = f"https://gitlab.com/api/v4/projects/{project_id}/packages/generic/{model_name}-model/{version}/{model_name}-model-{version}.tar.gz"

    headers = {"PRIVATE-TOKEN": token}
    response = requests.get(url, headers=headers)

    if response.status_code == 200:
        filename = f"{model_name}-model-{version}.tar.gz"
        with open(filename, 'wb') as f:
            f.write(response.content)

        # Extraire
        with tarfile.open(filename, 'r:gz') as tar:
            tar.extractall()

        print(f"Model {model_name} v{version} downloaded and extracted!")
    else:
        print(f"Error: {response.status_code} - {response.text}")

# Utilisation
download_model("nlu", "v1.0.0", "your-token", "12345")
```

## 🎮 Jobs Manuels

### Entraîner un modèle spécifique

Les jobs d'entraînement sont **manuels** pour éviter de consommer des ressources inutilement.

**Comment lancer :**
1. GitLab > CI/CD > Pipelines
2. Sélectionner le pipeline
3. Cliquer sur ▶️ pour le job voulu :
   - `train:nlu`
   - `train:stt`
   - `train:tts`

### Afficher les commandes de téléchargement

Les jobs `download:*` affichent les commandes curl pour télécharger les modèles.

**Comment lancer :**
1. GitLab > CI/CD > Pipelines
2. Sélectionner le pipeline d'un tag
3. Cliquer sur ▶️ pour :
   - `download:nlu`
   - `download:stt`
   - `download:tts`

## 🔄 Workflow de Release Complète

### Exemple : Release d'un nouveau modèle NLU

```bash
# 1. Développer sur une branche
git checkout -b feature/nlu-improvements
# ... modifications ...
git commit -am "feat(nlu): improve entity extraction"
git push origin feature/nlu-improvements

# 2. Créer une MR et merger vers main
# (via GitLab UI)

# 3. Entraîner le modèle sur main
# Aller dans GitLab > CI/CD > Pipelines
# Lancer manuellement train:nlu

# 4. Vérifier que le package est créé
# Le job package:nlu s'exécute automatiquement après train:nlu

# 5. Créer un tag pour release
git checkout main
git pull
git tag -a v1.1.0 -m "Release v1.1.0: Improved NLU entity extraction"
git push origin v1.1.0

# 6. Vérifier la release
# GitLab > Releases
# Le modèle est maintenant disponible avec la version v1.1.0
```

## 🎯 Stratégies de Déploiement

### Déploiement par environnement

Vous pouvez adapter le CI/CD pour déployer vers différents environnements :

```yaml
deploy:nlu:dev:
  stage: deploy
  script:
    - curl ... # download model
    - # deploy to dev environment
  environment:
    name: development
  only:
    - main

deploy:nlu:prod:
  stage: deploy
  script:
    - curl ... # download model
    - # deploy to production
  environment:
    name: production
  only:
    - tags
  when: manual
```

### Déploiement Blue/Green

```yaml
deploy:nlu:blue:
  stage: deploy
  script:
    - # deploy to blue environment
  environment:
    name: production-blue

deploy:nlu:green:
  stage: deploy
  script:
    - # deploy to green environment
  environment:
    name: production-green
  when: manual
```

## 🔧 Configuration Avancée

### Changer le registry

Modifier dans `.gitlab-ci.yml` :

```yaml
variables:
  MODEL_REGISTRY_URL: "https://your-custom-registry.com/models"
```

### Ajouter des tests de validation

```yaml
validate:nlu:
  stage: test
  dependencies:
    - train:nlu
  script:
    - python nlu/validate.py --model nlu/models/model_ner
    - python nlu/test_accuracy.py
```

### Notification sur succès/échec

```yaml
notify:success:
  stage: .post
  script:
    - curl -X POST https://slack.com/webhook ...
  when: on_success

notify:failure:
  stage: .post
  script:
    - curl -X POST https://slack.com/webhook ...
  when: on_failure
```

## 📊 Monitoring et Logs

### Voir les logs d'entraînement

1. GitLab > CI/CD > Pipelines
2. Cliquer sur le pipeline
3. Cliquer sur le job (ex: `train:nlu`)
4. Voir les logs en temps réel

### Télécharger les artifacts

1. GitLab > CI/CD > Pipelines
2. Cliquer sur le pipeline
3. Cliquer sur "Download" dans la colonne Artifacts
4. Les modèles sont dans l'archive

### Voir l'historique des versions

1. GitLab > Packages & Registries > Package Registry
2. Sélectionner un modèle
3. Voir toutes les versions disponibles

## 🎓 Best Practices

### 1. Versionning Sémantique

Utilisez [Semantic Versioning](https://semver.org/) pour vos tags :

- `v1.0.0` : Release majeure
- `v1.1.0` : Nouvelles features
- `v1.1.1` : Bug fixes

### 2. Commits Conventionnels

Utilisez [Conventional Commits](https://www.conventionalcommits.org/) :

```
feat(nlu): add support for multiple via cities
fix(stt): improve audio preprocessing
docs(readme): update installation instructions
```

### 3. Tests Avant Release

Toujours tester sur `main` avant de créer un tag :

```bash
# Sur main
git push origin main
# Attendre que les tests passent
# Lancer train:nlu manuellement
# Vérifier les résultats
# Ensuite créer le tag
git tag v1.0.0
```

### 4. Artifacts Retention

Les artifacts sont conservés :
- 1 jour pour les installations
- 7 jours pour les modèles entraînés
- 30 jours pour les packages

Ajustez dans `.gitlab-ci.yml` si nécessaire.

## 🆘 Troubleshooting

### Job bloqué en "pending"

**Cause** : Pas de runner disponible

**Solution** :
- Vérifier que des runners sont actifs dans Settings > CI/CD > Runners
- Vérifier les tags des runners correspondent aux tags des jobs

### Échec de l'upload vers le registry

**Cause** : Permissions insuffisantes

**Solution** :
- Vérifier que le projet a les packages activés (Settings > General > Visibility)
- Vérifier les permissions du token CI

### Modèle trop gros

**Cause** : Limite de taille des artifacts

**Solution** :
- Augmenter la limite dans Settings > CI/CD > General pipelines
- Utiliser un stockage externe (S3, GCS) pour les gros modèles

### Job de téléchargement échoue

**Cause** : Token expiré ou invalide

**Solution** :
- Générer un nouveau Personal Access Token
- Vérifier que le scope `read_api` est activé

## 📚 Ressources

- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)
- [GitLab Package Registry](https://docs.gitlab.com/ee/user/packages/package_registry/)
- [GitLab Release CLI](https://docs.gitlab.com/ee/ci/yaml/README.html#release)
