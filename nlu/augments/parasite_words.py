"""
Augmentation : Mots parasites
"""

import random
from typing import List, Dict


# Mots parasites courants en français
PARASITE_WORDS = [
    "genre",
    "style",
    "comme qui dirait",
    "disons",
    "enfin",
    "bon",
    "ben",
    "bah",
    "voilà quoi",
    "tu sais",
    "vous savez",
    "en gros",
    "grosso modo",
    "en quelque sorte",
    "si tu veux",
    "si vous voulez",
    "quelque part",
    "en fait",
    "franchement",
    "vraiment",
    "carrément",
    "littéralement",
    "pratiquement",
    "limite",
]


def augment_parasite_words(text: str, entities: Dict) -> List[tuple]:
    """
    Insère des mots parasites dans le texte

    Exemples:
    - "Je veux aller de Paris à Lyon" -> "Je veux genre aller de Paris à Lyon"
    - "Comment aller à Lyon" -> "Comment, enfin, aller à Lyon"
    - "Trajet Paris Lyon" -> "Trajet, tu sais, Paris Lyon"
    """
    variations = []

    words = text.split()
    if len(words) < 3:
        return variations

    # Insérer un mot parasite au début (15% de chance)
    if random.random() < 0.15:
        parasite = random.choice(PARASITE_WORDS)
        # Ajuster la casse
        new_text = f"{parasite.capitalize()} {text[0].lower() + text[1:]}"
        variations.append((new_text, entities.copy()))

    # Insérer un mot parasite au milieu (20% de chance)
    if random.random() < 0.20 and len(words) >= 3:
        parasite = random.choice(PARASITE_WORDS)
        # Position aléatoire au milieu (pas au début ni à la fin)
        insert_pos = random.randint(1, len(words) - 1)

        new_words = words.copy()
        # Ajouter des virgules si le parasite est court
        if len(parasite.split()) == 1:
            new_words.insert(insert_pos, f"{parasite},")
        else:
            new_words.insert(insert_pos, parasite)

        variations.append((" ".join(new_words), entities.copy()))

    # Insérer un mot parasite à la fin (15% de chance)
    if random.random() < 0.15:
        parasite = random.choice(PARASITE_WORDS)
        variations.append((f"{text} {parasite}", entities.copy()))

    # Insérer deux mots parasites (10% de chance)
    if random.random() < 0.10 and len(words) >= 4:
        parasite1 = random.choice(PARASITE_WORDS)
        parasite2 = random.choice(PARASITE_WORDS)

        new_words = words.copy()
        # Premier parasite au début ou début-milieu
        pos1 = random.randint(0, len(words) // 2)
        new_words.insert(pos1, parasite1)

        # Deuxième parasite au milieu-fin
        pos2 = random.randint(len(new_words) // 2, len(new_words))
        new_words.insert(pos2, parasite2)

        variations.append((" ".join(new_words), entities.copy()))

    return variations
