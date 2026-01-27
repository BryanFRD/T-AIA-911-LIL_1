package com.epitech.travelorder.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of available ML models with their MLflow identifiers
 */
@Getter
@AllArgsConstructor
public enum ModelType {
    NLU_INTENT("nlu-intent-onnx", "intent-classifier"),
    NLU_NER("nlu-ner-onnx", "named-entity-recognizer"),
    STT("stt-onnx-full", "speech-to-text"),
    TTS("tts-onnx-full", "text-to-speech");

    private final String mlflowName;
    private final String description;
}
