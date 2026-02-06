"""
Augmenteur NLU principal - Centralise toutes les techniques d'augmentation
"""

import random
from typing import List, Dict

from .case_variations import augment_case_variations
from .punctuation import augment_punctuation
from .fillers import augment_fillers
from .typos import augment_typos
from .synonyms import augment_synonyms
from .oral_transformations import augment_oral_transformations
from .word_order import augment_word_order
from .stuttering import augment_stuttering
from .sms_language import augment_sms_language
from .narrative_context import augment_narrative_context
from .parasite_words import augment_parasite_words
from .person_prefix import augment_person_prefix
from .accent_variants import augment_accent_variants
from .homophones import augment_homophones
from .entity_swap import augment_entity_swap
from .time_date_formats import augment_time_date_formats
from .politeness_variations import augment_politeness_variations
from .implicit_questions import augment_implicit_questions
from .city_variants import augment_city_variants


class NLUAugmenter:
    """
    Classe principale pour augmenter le dataset NLU

    Techniques disponibles :
    1. Variations de casse (majuscules/minuscules)
    2. Ajout de ponctuation variée
    3. Ajout de mots de remplissage (euh, hein, etc.)
    4. Fautes de frappe courantes
    5. Synonymes contextuels
    6. Transformations orales (contractions + abréviations: jveux, cmt, etc.)
    7. Inversions ordre des mots
    8. Répétitions et bégaiement (je je veux, tra- trajet)
    9. Langage SMS/chat (j ve alé, komen)
    10. Contexte narratif (alors, donc, voilà, etc.)
    11. Mots parasites (genre, enfin, tu sais, etc.)
    12. Interpellations de personnes (Hey Maude, Dis Simon, etc.)
    13. Variantes d'accents (retire 30% des accents: à->a, é->e, etc.)
    14. Homophones (erreurs grammaticales: à/a, où/ou, ce/se, etc.)
    15. Entity swap (inverse FROM <-> TO pour trajets retour)
    16. Formats temps/date (14h30 -> quatorze heures trente, demain -> le lendemain)
    17. Variations de politesse (je veux -> je voudrais, + s'il vous plaît)
    18. Questions implicites (Paris Lyon -> Paris Lyon ?, affirmation -> question)
    19. Variantes de noms de villes (Saint->St, tirets supprimés, abréviations)
    """

    def __init__(self):
        """Initialise l'augmenteur avec toutes les techniques disponibles"""
        self.techniques = [
            (augment_case_variations, 0.5),
            (augment_punctuation, 0.7),
            (augment_fillers, 0.6),
            (augment_typos, 0.4),
            (augment_synonyms, 0.3),
            (augment_oral_transformations, 0.4),  # Fusionne contractions + abbreviations
            (augment_word_order, 0.2),
            (augment_stuttering, 0.1),
            (augment_sms_language, 0.2),
            (augment_narrative_context, 0.4),
            (augment_parasite_words, 0.3),
            (augment_person_prefix, 0.3),
            (augment_accent_variants, 0.3),  # Retire 30% des accents aléatoirement
            (augment_homophones, 0.2),  # Erreurs homophones (à/a, où/ou, etc.)
            (augment_entity_swap, 0.15),  # Inverse FROM <-> TO pour trajets retour
            (augment_time_date_formats, 0.5),  # Variations de formats temps/date
            (augment_politeness_variations, 0.4),  # Variations de politesse
            (augment_implicit_questions, 0.2),  # Questions implicites
            (augment_city_variants, 0.4),  # Variantes de noms de villes (Saint->St, tirets, abréviations)
        ]

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
        for technique, probability in self.techniques:
            try:
                variants = technique(text, entities, probability)
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

            # Générer des variantes (pour tous les intents)
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
