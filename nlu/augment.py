"""
Module de data augmentation pour le NLU (français uniquement)

Techniques implémentées :
1. Variations de casse (majuscules/minuscules)
2. Ajout de ponctuation variée
3. Ajout de mots de remplissage (euh, hein, etc.)
4. Fautes de frappe courantes
5. Synonymes contextuels
6. Contractions orales françaises
7. Inversions ordre des mots
"""

import random
import re
from typing import List, Dict

class NLUAugmenter:
    """Classe pour augmenter le dataset NLU"""

    def __init__(self):
        # Mots de remplissage (fillers)
        self.fillers = [
            "euh", "hein", "ben", "bah", "alors", "donc", "voilà",
            "tu vois", "genre", "en fait", "quoi", "hein", "du coup"
        ]

        # Variations de ponctuation
        self.punctuation_variants = ["", ".", "!", "?", "...", " ?", " !", " ."]

        # Fautes de frappe courantes (patterns)
        self.typo_rules = {
            "aller": ["alé", "allé", "allez"],
            "veux": ["veu", "veut"],
            "de": ["dé", "de"],
            "à": ["a", "à"],
            "comment": ["comen", "comman"],
            "trajet": ["trajé", "trajais"],
        }

        # Variations orthographiques
        self.spelling_variants = {
            "via": ["via", "par", "en passant par"],
            "en passant par": ["via", "par", "en passant par"],
            "par": ["via", "par"],
        }

        # Synonymes pour variété
        self.synonyms = {
            "aller": ["me rendre", "voyager", "partir"],
            "veux": ["voudrais", "souhaite", "aimerais", "desire"],
            "comment": ["comment faire pour", "quel moyen pour", "quelle route pour"],
            "trajet": ["voyage", "itinéraire", "route", "parcours"],
        }

    def augment_case_variations(self, text: str, entities: Dict) -> List[tuple]:
        """
        Génère des variations de casse

        Exemples:
        - "je veux aller de paris à lyon"
        - "Je Veux Aller De Paris À Lyon"
        - "JE VEUX ALLER DE PARIS À LYON"
        """
        variations = []

        # Tout en minuscules
        variations.append((text.lower(), entities.copy()))

        # Première lettre en majuscule
        variations.append((text.capitalize(), entities.copy()))

        # Tout en majuscules (rare mais possible dans certains contextes)
        if random.random() < 0.1:  # 10% de chance
            variations.append((text.upper(), entities.copy()))

        # Majuscule après ponctuation
        if "?" in text or "!" in text:
            words = text.split()
            for i, word in enumerate(words):
                if i > 0 and words[i-1].endswith(("?", "!", ".")):
                    words[i] = word.capitalize()
            variations.append((" ".join(words), entities.copy()))

        return variations

    def augment_punctuation(self, text: str, entities: Dict) -> List[tuple]:
        """
        Ajoute des variations de ponctuation

        Exemples:
        - "Je veux aller de Paris à Lyon"
        - "Je veux aller de Paris à Lyon."
        - "Je veux aller de Paris à Lyon!"
        - "Je veux aller de Paris à Lyon?"
        """
        variations = []

        for punct in self.punctuation_variants:
            # Supprimer toute ponctuation existante à la fin
            clean_text = text.rstrip(".,!?… ")
            new_text = clean_text + punct
            variations.append((new_text, entities.copy()))

        return variations

    def augment_fillers(self, text: str, entities: Dict) -> List[tuple]:
        """
        Ajoute des mots de remplissage (hésitations naturelles)

        Exemples:
        - "Euh je veux aller de Paris à Lyon"
        - "Je veux aller euh de Paris à Lyon"
        - "Je veux aller de Paris à Lyon hein"
        """
        variations = []

        # 20% de chance d'ajouter un filler au début
        if random.random() < 0.2:
            filler = random.choice(self.fillers)
            new_text = f"{filler.capitalize()} {text[0].lower() + text[1:]}"
            variations.append((new_text, entities.copy()))

        # 15% de chance d'ajouter un filler au milieu
        if random.random() < 0.15:
            words = text.split()
            if len(words) > 3:
                insert_pos = random.randint(1, len(words) - 1)
                filler = random.choice(self.fillers)
                words.insert(insert_pos, filler)
                variations.append((" ".join(words), entities.copy()))

        # 15% de chance d'ajouter un filler à la fin
        if random.random() < 0.15:
            filler = random.choice(self.fillers)
            new_text = f"{text} {filler}"
            variations.append((new_text, entities.copy()))

        return variations

    def augment_typos(self, text: str, entities: Dict) -> List[tuple]:
        """
        Introduit des fautes de frappe courantes (sans toucher aux entités)

        Exemples:
        - "Je veu aller de Paris à Lyon"
        - "Je veux alé de Paris à Lyon"
        """
        variations = []

        # Ne pas modifier les noms de villes et prénoms
        protected_words = set()
        if entities.get("from"):
            protected_words.add(entities["from"].lower())
        if entities.get("to"):
            protected_words.add(entities["to"].lower())
        for via in entities.get("via", []):
            protected_words.add(via.lower())
        if entities.get("person"):
            protected_words.add(entities["person"].lower())

        words = text.split()
        modified = False

        for word_idx, word in enumerate(words):
            word_lower = word.lower().strip(".,!?")

            # Vérifier si le mot est protégé
            if word_lower in protected_words:
                continue

            # Appliquer les règles de typo
            if word_lower in self.typo_rules:
                if random.random() < 0.3:  # 30% de chance de faute
                    typo_variant = random.choice(self.typo_rules[word_lower])
                    # Conserver la casse originale
                    if word[0].isupper():
                        typo_variant = typo_variant.capitalize()
                    words[word_idx] = word.replace(word_lower, typo_variant, 1)
                    modified = True

        if modified:
            variations.append((" ".join(words), entities.copy()))

        return variations

    def augment_synonyms(self, text: str, entities: Dict) -> List[tuple]:
        """
        Remplace certains mots par des synonymes

        Exemples:
        - "Je voudrais aller de Paris à Lyon"
        - "Je souhaite me rendre de Paris à Lyon"
        """
        variations = []

        words = text.split()

        for word_idx, word in enumerate(words):
            word_lower = word.lower().strip(".,!?")

            if word_lower in self.synonyms:
                if random.random() < 0.4:  # 40% de chance de remplacement
                    synonym = random.choice(self.synonyms[word_lower])
                    # Conserver la casse
                    if word[0].isupper():
                        synonym = synonym.capitalize()

                    new_words = words.copy()
                    new_words[word_idx] = word.replace(word_lower, synonym, 1)
                    variations.append((" ".join(new_words), entities.copy()))

        return variations

    def augment_word_order(self, text: str, entities: Dict) -> List[tuple]:
        """
        Génère des inversions d'ordre naturelles en français

        Exemples:
        - "De Paris à Lyon, je veux aller"
        - "À Lyon je veux aller depuis Paris"
        """
        variations = []

        # Extraction des parties de la phrase
        if entities.get("from") and entities.get("to"):
            from_city = entities["from"]
            to_city = entities["to"]

            # Différents ordres possibles
            patterns = [
                f"De {from_city} à {to_city} comment aller",
                f"Pour aller de {from_city} à {to_city}",
                f"Un trajet de {from_city} vers {to_city}",
                f"{to_city} depuis {from_city} comment y aller",
            ]

            for pattern in patterns:
                if random.random() < 0.3:  # 30% de chance
                    variations.append((pattern, entities.copy()))

        return variations

    def augment_contractions(self, text: str, entities: Dict) -> List[tuple]:
        """
        Ajoute des contractions orales courantes

        Exemples:
        - "J'veux aller de Paris à Lyon"
        - "Chu parti de Paris pour Lyon"
        """
        variations = []

        contractions = {
            "je veux": ["j'veux", "chui", "j'aimerais"],
            "tu veux": ["tu veux", "t'veux"],
            "je suis": ["chui", "j'suis", "jsuis"],
            "il y a": ["y'a", "ya"],
        }

        text_lower = text.lower()
        for pattern, replacements in contractions.items():
            if pattern in text_lower:
                for replacement in replacements:
                    if random.random() < 0.3:  # 30% de chance
                        new_text = re.sub(
                            pattern,
                            replacement,
                            text,
                            flags=re.IGNORECASE
                        )
                        if new_text != text:
                            variations.append((new_text, entities.copy()))

        return variations

    def augment_example(self, text: str, entities: Dict,
                       num_variants: int = 3) -> List[tuple]:
        """
        Applique plusieurs techniques d'augmentation sur un exemple

        Args:
            text: Le texte original
            entities: Les entités extraites
            num_variants: Nombre de variantes à générer

        Returns:
            Liste de tuples (texte_augmenté, entités)
        """
        all_variants = [(text, entities)]  # Inclure l'original

        # Appliquer chaque technique
        techniques = [
            self.augment_case_variations,
            self.augment_punctuation,
            self.augment_fillers,
            self.augment_typos,
            self.augment_synonyms,
            self.augment_contractions,
            self.augment_word_order,
        ]

        for technique in techniques:
            try:
                variants = technique(text, entities)
                all_variants.extend(variants)
            except Exception as e:
                print(f"Warning: {technique.__name__} failed: {e}")
                continue

        # Dédupliquer et limiter
        seen = set()
        unique_variants = []
        for variant_text, variant_entities in all_variants:
            if variant_text not in seen:
                seen.add(variant_text)
                unique_variants.append((variant_text, variant_entities))

        # Mélanger et retourner num_variants exemples
        random.shuffle(unique_variants)
        return unique_variants[:num_variants]

    def augment_dataset(self, dataset: List[Dict],
                       augmentation_factor: float = 2.0) -> List[Dict]:
        """
        Augmente un dataset complet

        Args:
            dataset: Dataset original
            augmentation_factor: Facteur d'augmentation (2.0 = double la taille)

        Returns:
            Dataset augmenté
        """
        augmented = []

        variants_per_example = max(1, int(augmentation_factor))

        for example in dataset:
            # Ajouter l'exemple original
            augmented.append(example)

            # Générer des variantes seulement pour les exemples "trajet"
            if example["intent"] == "trajet":
                variants = self.augment_example(
                    example["text"],
                    example["entities"],
                    num_variants=variants_per_example
                )

                for variant_text, variant_entities in variants[1:]:  # Skip original
                    augmented.append({
                        "text": variant_text,
                        "intent": example["intent"],
                        "entities": variant_entities
                    })

        print(f"\nAugmentation:")
        print(f"  - Dataset original: {len(dataset)} exemples")
        print(f"  - Dataset augmenté: {len(augmented)} exemples")
        print(f"  - Facteur réel: {len(augmented) / len(dataset):.2f}x")

        return augmented


if __name__ == "__main__":
    # Test de l'augmenteur
    augmenter = NLUAugmenter()

    test_text = "Je veux aller de Paris à Lyon"
    test_entities = {
        "from": "Paris",
        "to": "Lyon",
        "via": [],
        "person": ""
    }

    print("Texte original:", test_text)
    print("\nVariantes générées:")
    print("="*70)

    variants = augmenter.augment_example(test_text, test_entities, num_variants=10)

    for i, (variant_text, variant_entities) in enumerate(variants, 1):
        print(f"\n{i}. {variant_text}")
        if variant_entities != test_entities:
            print(f"   Entités modifiées: {variant_entities}")
