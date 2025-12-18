# TODO - Optimisations Système NLU

**Baseline actuelle**: 68.2% (30/44 tests)
**Objectif**: 88-95% (39-42/44 tests)
**Gain estimé**: +20-27 points

---

## 📌 PHASE 1: BUGS CRITIQUES (Priorité IMMÉDIATE - 1h)

### ✅ 1.1 Templates hardcodés (nlu/data/templates.json)
- [ ] Remplacer "Je vais Paris à Lyon" → "Je vais {from_city} à {to_city}" (ligne 106)
- [ ] Remplacer "Trajet pour aller Paris" → "Trajet pour aller {to_city}" (ligne 107)
- [ ] Remplacer "De Paris je veux Lyon" → "De {from_city} je veux {to_city}" (ligne 108)
- [ ] Remplacer "Aller Lyon depuis Paris" → "Aller {to_city} depuis {from_city}" (ligne 109)
- [ ] Remplacer "Je dois Paris Lyon" → "Je dois {from_city} {to_city}" (ligne 110)
- [ ] Remplacer "Faire le trajet Paris vers Lyon" → "Faire le trajet {from_city} vers {to_city}" (ligne 111)

**Impact**: Élimine doublons Paris-Lyon, augmente diversité

---

### ✅ 1.2 Villes dupliquées (nlu/data/cities.json)
- [ ] Dédupliquer 14 villes en double: Vincennes, Villeurbanne, Roubaix, Quimper, Périgueux, Nanterre, Metz, Lyon, Courbevoie, Chartres, Bordeaux, Bastia

**Script**:
```python
import json
with open("nlu/data/cities.json") as f:
    cities = json.load(f)
unique_cities = sorted(list(set(cities)))
with open("nlu/data/cities.json", "w") as f:
    json.dump(unique_cities, f, indent=2, ensure_ascii=False)
```

**Impact**: 564 → 550 villes uniques

---

### ✅ 1.3 Bug probabilité inversée (nlu/augments/punctuation.py)
- [ ] Ligne 25: Changer `if random.random() > probability` en `if random.random() < probability`

**Impact**: Active 70% au lieu de 30%

---

### ✅ 1.4 Bug probabilité inversée (nlu/augments/word_order.py)
- [ ] Ligne 13: Changer `if random.random() > probability` en `if random.random() < probability`

**Impact**: Active 20% au lieu de 80%

---

### ✅ 1.5 Bugs indexation unsafe (nlu/augments/)
- [ ] **fillers.py ligne 32**: Ajouter check `if len(text) > 1` avant `text[0].lower() + text[1:]`
- [ ] **narrative_context.py lignes 71, 83**: Ajouter même check

**Impact**: Évite crash sur texte court

---

### ✅ 1.6 Bug merge VIA (nlu/predict.py)
- [ ] Lignes 79-86: Remplacer substring matching par tracking d'index de tokens

**Code**:
```python
def _extract_entities_from_ner(self, ner_result):
    entities = {"from": "", "to": "", "via": [], "person": ""}
    last_via_idx = -1

    for idx, r in enumerate(ner_result):
        entity_type = r.get("entity_group", r.get("entity")).replace("B-", "").replace("I-", "")
        word = r["word"].replace("▁", " ").strip()

        if entity_type == "FROM":
            entities["from"] += (" " + word if entities["from"] else word)
        elif entity_type == "TO":
            entities["to"] += (" " + word if entities["to"] else word)
        elif entity_type == "VIA":
            if idx == last_via_idx + 1:  # Token consécutif
                entities["via"][-1] += " " + word
            else:  # Nouvelle entité VIA
                entities["via"].append(word)
            last_via_idx = idx
        elif entity_type == "PERSON":
            entities["person"] += (" " + word if entities["person"] else word)

    entities["from"] = entities["from"].strip()
    entities["to"] = entities["to"].strip()
    entities["via"] = [v.strip() for v in entities["via"]]
    entities["person"] = entities["person"].strip()

    return entities
```

**Impact**: Fixe "Dijon" in "Dijon-les-Bains" bug

---

### ✅ 1.7 Protection entités (nlu/augments/contractions.py)
- [ ] Lignes 43-48: Protéger entités avant regex replace

**Code**:
```python
def augment_contractions(text: str, entities: Dict, probability: float) -> List[tuple]:
    if random.random() > probability:
        return []

    # Extraire mots protégés des entités
    protected = set()
    if entities.get("from"):
        protected.update(entities["from"].lower().split())
    if entities.get("to"):
        protected.update(entities["to"].lower().split())
    if entities.get("via"):
        for v in entities["via"]:
            protected.update(v.lower().split())

    words = text.split()
    result = []
    for word in words:
        if word.lower() not in protected:
            # Appliquer contractions
            word = word.replace("je veux", "j'veux")
            word = word.replace("je suis", "j'suis")
            # ... autres contractions
        result.append(word)

    return [(" ".join(result), entities)]
```

**Impact**: Évite corruption entités

---

## 📌 PHASE 2: HAUTE PRIORITÉ (Court terme - 2-3h)

### 🔄 2.1 Normalisation noms de villes (nlu/predict.py)
- [ ] Ajouter import `from difflib import SequenceMatcher`
- [ ] Charger cities.json dans `__init__` (après ligne 51)
- [ ] Créer index `cities_lower = {city.lower(): city for city in cities}`
- [ ] Ajouter méthode `_normalize_city_name()` avec fuzzy matching (seuil 85%)
- [ ] Appliquer dans `_extract_entities_from_ner()` avant return

**Impact**: Corrige "pArIs" → "Paris", "Lyin" → "Lyon"

---

### 🔄 2.3 Multi-Task Learning (nlu/train.py)
- [ ] Créer classe `MultiTaskNLU(nn.Module)` avec encodeur partagé
- [ ] Tête NER (token-level) + tête Intent (sequence-level)
- [ ] Loss combinée: `0.6 * ner_loss + 0.4 * intent_loss`
- [ ] Remplacer entraînements séparés par entraînement joint

**Impact**: +3-7% sur les deux tâches

---

## 📌 PHASE 3: MOYENNE PRIORITÉ (Moyen terme - 4-6h)

### ⚙️ 3.1 Supprimer case_variations
- [ ] Commenter ligne 46 dans augmenter.py

**Raison**: Impact minimal

---

### ⚙️ 3.2 Réduire probabilité punctuation
- [ ] Ligne 47 augmenter.py: 0.7 → 0.4

**Raison**: Trop agressive

---

### ⚙️ 3.3 Ajouter 20 nouveaux templates
- [ ] 2 négations: "Je ne veux pas aller à {to_city}", "Je n'irai pas à {to_city}"
- [ ] 3 retours: "Aller-retour {from_city} {to_city}", etc.
- [ ] 3 transports: "Train de {from_city} à {to_city}", etc.
- [ ] 3 préférences: "Le plus rapide vers {to_city}", etc.
- [ ] 3 temporels: "Dans 3 jours je vais à {to_city}", etc.
- [ ] 5 négations subtiles: "{city} est très belle", "Combien d'habitants à {city}", etc.

**Impact**: +diversité patterns

---

### ⚙️ 3.4 Validation BIO après augmentation
- [x] Créer fonction `validate_entities_preserved()`
- [ ] Vérifier entités présentes dans texte augmenté
- [ ] Appliquer dans augmenter.py ligne 81

**Impact**: Qualité données augmentées

---

### ⚙️ 3.5 Logging augmentations
- [ ] Ajouter `from collections import Counter`
- [ ] Tracker stats par augmentation
- [ ] Logger à la fin de augment_dataset

**Impact**: Visibilité efficacité augmentations

---

### 🆕 4.2 Labels temporels (DATE/TIME)
- [ ] Ajouter B-DATE, I-DATE, B-TIME, I-TIME dans config.json
- [ ] Créer templates avec {date} et {time}
- [ ] Adapter generate_from_templates.py pour labeling temporel
- [ ] Adapter predict.py pour extraction DATE/TIME

---

## 📊 SUIVI PROGRESSION

### Métriques Baseline
- Tests réussis: 30/44 (68.2%)
- Intent: 44/44 (100.0%)
- FROM: 38/44 (86.4%)
- TO: 41/44 (93.2%)
- VIA: 42/44 (95.5%)
- PERSON: 37/44 (84.1%)

### Objectifs
- [ ] Phase 1 complète → +5-10% (73-78%)
- [ ] Phase 2 complète → +10-20% (83-88%)
- [ ] Phase 3 complète → +3-5% (86-93%)
- [ ] Phase 4 complète → +5-10% robustesse (88-95%)

---

## 🔧 COMMANDES UTILES

### Tester après modifications
```bash
python nlu/generate_from_templates.py
python nlu/train.py
python nlu/predict.py
```

### Commit format
```bash
git add <fichiers>
git commit -m "fix(nlu): <description>

<détails>

🤖 Generated with Claude Code

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push
```

---

**Dernière mise à jour**: 2025-12-17
**Temps estimé total**: 13-18h développement
