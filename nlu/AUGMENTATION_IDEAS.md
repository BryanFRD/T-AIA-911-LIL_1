# Idées d'augmentation de données supplémentaires

## ✅ Déjà implémenté

1. ✅ Variations de casse
2. ✅ Variations de ponctuation
3. ✅ Mots de remplissage (euh, hein, etc.)
4. ✅ Fautes de frappe courantes
5. ✅ Synonymes contextuels
6. ✅ Contractions orales françaises
7. ✅ Inversions d'ordre des mots

## 🔮 Idées futures

### 1. Variations géographiques
Ajouter des références régionales :
- "Je veux prendre le train de Paris à Lyon"
- "Un TGV de Paris à Lyon"
- "La ligne Paris-Lyon"
- "Le direct Paris Lyon"

### 2. Contexte temporel
Ajouter des références temporelles (qui seront ignorées) :
- "Je veux aller de Paris à Lyon demain"
- "Aller de Paris à Lyon ce soir"
- "Trajet Paris-Lyon pour lundi"

### 3. Mots parasites supplémentaires
- "s'il vous plaît"
- "s'il te plaît"
- "merci"
- "rapidement"
- "urgent"

Exemples :
- "Je veux aller de Paris à Lyon s'il vous plaît"
- "Trajet urgent de Paris à Lyon"

### 4. Variations de formulation
Ajouter plus de templates avec :
- Questions indirectes : "Je me demande comment aller de Paris à Lyon"
- Conditionnel : "J'aurais besoin d'aller de Paris à Lyon"
- Impératif : "Trouve-moi un trajet de Paris à Lyon"
- Négation suivie d'affirmation : "Je veux pas rester ici, aller à Lyon depuis Paris"

### 5. Émojis et caractères spéciaux
(Pour chatbots / interfaces modernes)
- "Je veux aller de Paris à Lyon 🚄"
- "Paris -> Lyon ?"
- "Paris => Lyon"

### 6. Abréviations courantes
- "Je vx aller de Paris à Lyon" (veux -> vx)
- "Cmt aller de Paris à Lyon" (comment -> cmt)
- "Tjrs de Paris à Lyon" (toujours -> tjrs)

### 7. Répétitions et bégaiements
(Simule le langage oral naturel)
- "Je je veux aller de Paris à Lyon"
- "Je veux veux aller de Paris à Lyon"
- "Aller aller de Paris à Lyon"

### 8. Langage SMS/chat
- "chui a paris je vx aler a lyon"
- "c koi le traj pr aler de paris a lyon"

### 9. Variations de politesse
Différents niveaux de formalité :
- Formel : "Je souhaiterais obtenir un itinéraire de Paris à Lyon"
- Familier : "Faut que j'aille de Paris à Lyon"
- Très familier : "J'dois me casser à Lyon depuis Paris"

### 10. Contexte narratif
Phrases avec contexte supplémentaire :
- "Mon frère habite à Lyon, comment y aller depuis Paris"
- "Je dois aller voir ma famille à Lyon en partant de Paris"
- "Rendez-vous à Lyon, je pars de Paris"

### 11. Erreurs de ville proches
(Pour tester la robustesse)
- "Je veux aller de Paris à Lion" (Lyon -> Lion)
- "Trajet Paris Lyone" (Lyon -> Lyone)

### 12. Back-translation (via paraphrase)
Utiliser un modèle de paraphrase français pour générer des variantes naturelles :
- Original : "Je veux aller de Paris à Lyon"
- Paraphrase : "Mon souhait est de me déplacer de Paris vers Lyon"

## 🎯 Priorités recommandées

### Haute priorité
1. ✅ Mots de remplissage (déjà fait)
2. ✅ Fautes de frappe (déjà fait)
3. **Variations géographiques** (contexte métier)
4. **Mots parasites** (améliore robustesse)

### Priorité moyenne
5. **Contexte temporel** (fréquent dans les requêtes réelles)
6. **Variations de formulation** (plus de templates)
7. **Répétitions** (langage oral)

### Basse priorité (nice-to-have)
8. Langage SMS (peut dégrader la qualité)
9. Émojis (dépend du canal)
10. Back-translation (coût computationnel élevé)

## 📊 Comment mesurer l'impact

Pour chaque technique, mesurer :
1. **Augmentation du dataset** : combien d'exemples supplémentaires
2. **Précision sur corpus de test** : amélioration de la performance
3. **Robustesse** : performance sur des phrases "difficiles"
4. **Over-fitting** : vérifier que le modèle ne mémorise pas

## 💡 Implémentation progressive

1. **Phase 1** (actuel) : Variations de base (casse, ponctuation, synonymes)
2. **Phase 2** : Contexte métier (géographie, temps, mots parasites)
3. **Phase 3** : Langage naturel avancé (répétitions, formulations)
4. **Phase 4** : Techniques avancées (paraphrase, back-translation)

## ⚠️ Attention

- **Ne pas sur-augmenter** : risque de diluer les vrais patterns
- **Garder un équilibre** : 50-70% original, 30-50% augmenté
- **Valider manuellement** : vérifier que les augmentations ont du sens
- **Tester régulièrement** : mesurer l'impact réel sur les performances
