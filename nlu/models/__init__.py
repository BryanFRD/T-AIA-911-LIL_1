"""
NLU Models Module

Custom model architectures for NLU tasks
"""

from .ner_crf import CamembertForTokenClassificationWithCRF

__all__ = [
    "CamembertForTokenClassificationWithCRF",
]
