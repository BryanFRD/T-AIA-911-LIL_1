# NLU - TODO & Améliorations

## 📊 Dataset & Templates

### Templates
- [ ] **Ajouter des templates avec erreurs de grammaire**
  - "Je vais Paris à Lyon" (inversion)
  - "Trajet pour aller Paris" (manque préposition)
  - "De Paris je veux Lyon" (ordre incorrect)

- [ ] **Ajouter des templates avec formulations régionales**
  - "Je pars sur Paris" (sur au lieu de à)
  - "Je monte à Paris" (régionalisme)
  - "Je descends à Lyon" (régionalisme)

- [ ] **Ajouter des templates plus conversationnels**
  - "Ouais je cherche à aller de {from_city} vers {to_city}"
  - "Attends, c'est quoi le trajet {from_city} {to_city} ?"
  - "Genre je voudrais partir de {from_city}"

- [ ] **Ajouter des templates avec contexte temporel**
  - "Demain je veux aller de {from_city} à {to_city}"
  - "Ce soir trajet {from_city} {to_city}"
  - "Dans 2h je pars de {from_city}"

- [ ] **Ajouter des templates avec plusieurs destinations via**
  - Actuellement: 2 villes max en via
  - Objectif: Supporter 3-4 villes via
  - "De {from_city} à {to_city} en passant par {via_city1}, {via_city2} et {via_city3}"

### Villes
- [ ] **Ajouter les petites villes françaises** (actuellement 564 villes)
  - Objectif: 1000+ villes
  - Inclure toutes les préfectures et sous-préfectures
  - Ajouter les villes de banlieue importantes

- [ ] **Gérer les variantes de noms de villes**
  - "Paris" vs "Paname" (argot)
  - "Marseille" vs "Massilia"
  - Noms avec tirets vs sans tirets

- [ ] **Ajouter des lieux non-villes**
  - Aéroports: "CDG", "Orly", "Roissy"
  - Gares: "Gare du Nord", "Gare de Lyon"
  - Quartiers: "La Défense", "Montmartre"

### Entités supplémentaires
- [ ] **Ajouter entité TIME (heure/date)**
  - "Je veux aller de Paris à Lyon demain à 14h"
  - Nécessite nouveau label: B-TIME, I-TIME

- [ ] **Ajouter entité TRANSPORT_MODE**
  - "Je veux aller de Paris à Lyon en train"
  - Labels: B-MODE, I-MODE
  - Valeurs: train, voiture, bus, avion, covoiturage

## 🔄 Augmentation de données

### Nouvelles techniques d'augmentation

- [ ] **Ajout de bruits audio transcrits**
  - Répétitions de syllabes: "pa-pa-paris"
  - Sons parasites: "euh... de Paris"
  - Hésitations multiples: "je... euh... enfin... veux aller"

- [ ] **Variations dialectales**
  - Nord: "Ch'veux aller à Lille"
  - Sud: "Vé, je veux aller à Marseille"
  - Québécois: "Je veux aller à Paris là"

- [ ] **Insertions de mots de politesse**
  - "S'il vous plaît, trajet Paris Lyon"
  - "Merci de me donner le trajet de Paris à Lyon"
  - "Bonjour, je voudrais aller de Paris à Lyon"

- [ ] **Variations d'intensité émotionnelle**
  - Urgent: "VITE trajet Paris Lyon !"
  - Calme: "Tranquillement, je cherche Paris Lyon"
  - Frustré: "Bon allez quoi, Paris Lyon !"

- [ ] **Code-switching (français-anglais)**
  - "Je want aller de Paris à Lyon"
  - "Trajet from Paris to Lyon"
  - "Je veux go Paris Lyon"

- [ ] **Augmentation par reformulation avec LLM**
  - Utiliser un LLM pour générer des paraphrases
  - Maintenir les entités exactes
  - Augmenter la diversité naturelle

### Amélioration des augmentations existantes

- [ ] **Typos plus réalistes**
  - Basés sur la proximité clavier AZERTY
  - Doubles frappes: "Parris" au lieu de "Paris"
  - Lettres manquantes: "Pais" au lieu de "Paris"

- [ ] **SMS language plus complet**
  - Dictionnaire plus large
  - Variations: "jveu", "jvé", "jve"
  - Acronymes: "rdv" pour rendez-vous

## 🧠 Architecture du modèle

### Optimisations

- [ ] **Fine-tuning avec apprentissage par curriculum**
  - Commencer avec exemples simples
  - Progressivement ajouter exemples complexes
  - Améliore la convergence

- [ ] **Augmenter les epochs selon la taille du dataset**
  - Actuellement: 5 epochs
  - Pour 4461 exemples: tester 8-10 epochs
  - Monitoring de l'overfitting

- [ ] **Learning rate scheduling**
  - Actuellement: learning_rate fixe (5e-5)
  - Ajouter warmup (10% des steps)
  - Decay cosine ou linéaire

- [ ] **Ajuster batch size selon GPU**
  - Actuellement: batch_size=8
  - RTX 2060 SUPER (8GB): tester 16 ou 32
  - Calcul automatique selon mémoire disponible

- [ ] **Mixed precision training (FP16)**
  - Accélération 2-3x sur GPU
  - Économie de mémoire GPU
  - Ajouter à TrainingArguments: `fp16=True`

### Architecture avancée

- [ ] **Tester d'autres modèles de base**
  - CamemBERT-large (plus précis mais plus lent)
  - FlauBERT (alternative française)
  - mBERT multilingue (si besoin futur)

- [ ] **Multi-task learning**
  - Entraîner NER + Intent simultanément
  - Partage des représentations
  - Meilleure généralisation

- [ ] **Conditional Random Fields (CRF) layer**
  - Ajouter CRF au-dessus de BERT
  - Améliore cohérence des séquences BIO
  - Évite B-TO suivi de I-FROM

## 📈 Évaluation & Métriques

- [ ] **Créer un dataset de validation séparé**
  - Actuellement: pas de validation split
  - Split 80/20 train/val
  - Early stopping sur validation loss

- [ ] **Créer un dataset de test avec exemples réels**
  - Collecter vraies requêtes utilisateurs
  - Annoter manuellement
  - ~200-300 exemples minimum

- [ ] **Métriques détaillées par entité**
  - F1-score pour FROM, TO, VIA, PERSON séparément
  - Confusion matrix
  - Erreurs fréquentes (Paris reconnu comme TO au lieu de FROM)

- [ ] **Métriques d'intent**
  - Précision/Rappel pour "trajet" vs "other"
  - Faux positifs (autres reconnus comme trajet)
  - Faux négatifs (trajets non détectés)

- [ ] **Tests de robustesse**
  - Villes inconnues (hors dataset)
  - Formulations très atypiques
  - Inputs très longs (>100 tokens)
  - Inputs très courts (2-3 mots)

## 🚀 Performance & Déploiement

### Optimisations d'inférence

- [ ] **Quantization (INT8)**
  - Réduction taille modèle ~4x
  - Accélération inférence ~2-3x
  - Trade-off précision acceptable

- [ ] **ONNX conversion**
  - Export vers ONNX Runtime
  - Inférence plus rapide
  - Compatible avec plus de plateformes

- [ ] **Distillation de modèle**
  - CamemBERT-base → DistilCamemBERT
  - ~2x plus rapide, 40% plus léger
  - ~95% de précision conservée

- [ ] **Batching d'inférence**
  - Actuellement: 1 requête à la fois
  - Implémenter batch inference
  - Throughput accru pour API

### Caching & Optimisations

- [ ] **Cache des prédictions fréquentes**
  - "Paris Lyon" demandé souvent
  - Redis/LRU cache
  - TTL configurable

- [ ] **Pré-calcul des embeddings de villes**
  - Stocker embeddings BERT des 564 villes
  - Matching rapide par similarité cosine
  - Fallback si NER rate la ville

## 🔧 Infrastructure & Tooling

### Monitoring

- [ ] **Logging des prédictions**
  - Sauvegarder inputs/outputs
  - Analyse des erreurs en production
  - Réentraînement avec vraies données

- [ ] **Métriques en temps réel**
  - Latence (p50, p95, p99)
  - Taux d'erreur
  - Distribution des entités détectées

- [ ] **A/B testing**
  - Comparer versions de modèles
  - Metrics: précision, latence
  - Déploiement progressif (canary)

### Versioning & CI/CD

- [ ] **Model versioning sémantique**
  - v1.0.0: modèle initial
  - v1.1.0: nouvelles features (TIME, MODE)
  - v1.0.1: bugfixes, optimisations

- [ ] **Automated model evaluation**
  - CI pipeline teste sur dataset de test
  - Seuil minimum de F1-score
  - Reject si régression >5%

- [ ] **Model registry GitLab**
  - Actuellement: artifacts 30 jours
  - Stocker modèles long-terme
  - Métadonnées: métriques, dataset version

## 🧪 Expérimentations

### Features avancées

- [ ] **Few-shot learning pour nouvelles villes**
  - Ajouter une ville avec 5-10 exemples
  - Mise à jour sans réentraînement complet
  - Adapter layers finaux seulement

- [ ] **Active learning**
  - Identifier exemples incertains (confidence faible)
  - Demander annotation humaine
  - Réentraîner sur nouveaux exemples

- [ ] **Ensemble de modèles**
  - CamemBERT + FlauBERT + mBERT
  - Vote majoritaire ou stacking
  - Amélioration robustesse +2-3%

### Multilinguisme

- [ ] **Support anglais**
  - Templates: "I want to go from {from_city} to {to_city}"
  - Modèle: mBERT ou XLM-R
  - Même architecture, dataset parallèle

- [ ] **Code-mixing français-arabe**
  - Régions avec forte population arabophone
  - "Je veux aller min Paris l Lyon" (min = de)

## 📚 Documentation

- [ ] **Documenter format des templates**
  - Syntaxe des placeholders
  - Règles de génération
  - Exemples complets

- [ ] **Guide d'ajout de nouvelles entités**
  - Étapes: labels → templates → generator → train
  - Exemples: TIME, MODE

- [ ] **API documentation**
  - Swagger/OpenAPI spec
  - Exemples de requêtes
  - Codes erreur

- [ ] **Performance benchmarks**
  - Latence par taille d'input
  - Throughput GPU vs CPU
  - Consommation mémoire

---

## 🎯 Priorités (ordre recommandé)

### Phase 1 - Quick Wins (1-2 semaines)
1. ✅ Créer dataset de validation (80/20 split)
2. ✅ Métriques détaillées par entité
3. ✅ Ajouter 200+ villes (→ 800 villes)
4. ✅ Nouvelles augmentations (bruits audio, politesse)
5. ✅ Mixed precision training (FP16)

### Phase 2 - Amélioration qualité (3-4 semaines)
1. ✅ Collecter dataset de test réel (200-300 exemples)
2. ✅ Ajouter templates avec erreurs de grammaire
3. ✅ Ajouter entité TIME
4. ✅ CRF layer pour meilleure cohérence
5. ✅ Learning rate scheduling

### Phase 3 - Performance & Scale (4-6 semaines)
1. ✅ ONNX conversion
2. ✅ Quantization INT8
3. ✅ Batching d'inférence
4. ✅ Cache Redis des prédictions fréquentes
5. ✅ Model monitoring en production

### Phase 4 - Features avancées (2-3 mois)
1. ✅ Entité TRANSPORT_MODE
2. ✅ Multi-task learning NER+Intent
3. ✅ Distillation de modèle
4. ✅ Active learning pipeline
5. ✅ Support anglais

---

**Dernière mise à jour:** 2025-12-17
**Version du modèle:** v1.0.0 (baseline)
