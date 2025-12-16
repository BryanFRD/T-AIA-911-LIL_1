import json
import re
from pathlib import Path
from typing import List, Tuple, Dict

class CityGazetteer:
    """
    Gazetteer de villes pour améliorer la reconnaissance d'entités.
    Combine une liste de villes connues avec une recherche intelligente.
    """

    def __init__(self, cities_file="nlu/data/cities.json"):
        """Charge la liste des villes"""
        cities_path = Path(cities_file)
        if cities_path.exists():
            with open(cities_path, 'r', encoding='utf-8') as f:
                self.cities = json.load(f)
        else:
            self.cities = []

        # Créer des versions normalisées pour la recherche
        self.cities_normalized = {self._normalize(city): city for city in self.cities}

    def _normalize(self, text: str) -> str:
        """Normalise un texte pour la comparaison"""
        return text.lower().replace('-', ' ').replace("'", ' ')

    def find_cities_in_text(self, text: str) -> List[Dict[str, any]]:
        """
        Trouve toutes les villes mentionnées dans un texte

        Returns:
            Liste de dict avec {city, start, end}
        """
        matches = []
        text_lower = text.lower()

        # Recherche de chaque ville
        for city_norm, city_original in self.cities_normalized.items():
            # Chercher la ville avec ses différentes variantes
            patterns = [
                re.escape(city_original.lower()),
                re.escape(city_norm),
                re.escape(city_original.lower().replace('-', ' ')),
            ]

            for pattern in set(patterns):  # Éviter les doublons
                # Utiliser word boundaries pour éviter les faux positifs
                regex = r'\b' + pattern + r'\b'
                for match in re.finditer(regex, text_lower):
                    matches.append({
                        'city': city_original,
                        'start': match.start(),
                        'end': match.end(),
                        'matched_text': text[match.start():match.end()]
                    })

        # Supprimer les chevauchements (garder le plus long match)
        matches = self._remove_overlapping_matches(matches)
        return sorted(matches, key=lambda x: x['start'])

    def _remove_overlapping_matches(self, matches: List[Dict]) -> List[Dict]:
        """Supprime les matchs qui se chevauchent, garde le plus long"""
        if not matches:
            return []

        # Trier par position puis par longueur décroissante
        sorted_matches = sorted(matches, key=lambda x: (x['start'], -(x['end'] - x['start'])))

        result = []
        last_end = -1

        for match in sorted_matches:
            if match['start'] >= last_end:
                result.append(match)
                last_end = match['end']

        return result

    def extract_entities_with_context(self, text: str) -> Dict[str, any]:
        """
        Extrait les entités FROM/TO/VIA en utilisant le contexte et le gazetteer

        Args:
            text: Le texte à analyser

        Returns:
            Dict avec from, to, via
        """
        cities = self.find_cities_in_text(text)

        if not cities:
            return {"from": "", "to": "", "via": []}

        text_lower = text.lower()
        entities = {"from": "", "to": "", "via": []}

        # Patterns pour FROM
        from_patterns = [
            r'\bde\s+',
            r'\bdepuis\s+',
            r'\bpartir de\s+',
            r'\bparts? de\s+',
            r'\bviens de\s+',
        ]

        # Patterns pour TO
        to_patterns = [
            r'\bà\s+',
            r'\bpour\s+',
            r'\bvers\s+',
            r'\bdestination\s+',
            r'\baller à\s+',
        ]

        # Patterns pour VIA
        via_patterns = [
            r'\ben passant par\s+',
            r'\bvia\s+',
            r'\bpar\s+',
        ]

        # Analyser chaque ville trouvée
        for city_info in cities:
            city = city_info['city']
            start = city_info['start']

            # Extraire le contexte avant la ville (50 caractères)
            context_start = max(0, start - 50)
            context = text_lower[context_start:start]

            # Vérifier les patterns
            is_from = any(re.search(pattern + r'[^\s]*$', context) for pattern in from_patterns)
            is_to = any(re.search(pattern + r'[^\s]*$', context) for pattern in to_patterns)
            is_via = any(re.search(pattern + r'[^\s]*$', context) for pattern in via_patterns)

            # Assigner selon le contexte
            if is_via:
                entities["via"].append(city)
            elif is_from and not entities["from"]:
                entities["from"] = city
            elif is_to and not entities["to"]:
                entities["to"] = city
            elif not entities["from"]:
                # Si aucun pattern trouvé, la première ville est probablement FROM
                entities["from"] = city
            elif not entities["to"]:
                # La deuxième est probablement TO
                entities["to"] = city

        return entities


if __name__ == "__main__":
    # Test du gazetteer
    gazetteer = CityGazetteer()

    test_texts = [
        "Je veux aller de Paris à Lyon",
        "Comment me rendre à Port-Boulet depuis la gare de Tours ?",
        "Je veux aller de Marseille à Nice en passant par Cannes",
        "Aller de Lille à Strasbourg en passant par Reims",
        "Je pars de Bordeaux pour aller à La Rochelle via Rochefort",
        "Voyage de Paris à Monaco en passant par Lyon et Marseille"
    ]

    print("="*70)
    print("TEST DU GAZETTEER")
    print("="*70)

    for text in test_texts:
        print(f"\n> Texte : {text}")

        # Trouver les villes
        cities = gazetteer.find_cities_in_text(text)
        print(f"  Villes trouvees : {[c['city'] for c in cities]}")

        # Extraire avec contexte
        entities = gazetteer.extract_entities_with_context(text)
        print(f"  FROM: {entities['from'] or '(vide)'}")
        print(f"  TO: {entities['to'] or '(vide)'}")
        print(f"  VIA: {', '.join(entities['via']) or '(vide)'}")
