package com.epitech.travelorder.common.ml;

/**
 * Enumeration of available ML models with their MLflow identifiers
 */
public enum ModelType {
    NLU_INTENT("nlu-intent-onnx", "intent-classifier"),
    NLU_NER("nlu-ner-onnx", "named-entity-recognizer"),
    STT("stt-onnx-full", "speech-to-text"),
    TTS("tts-onnx-full", "text-to-speech");

    private final String mlflowName;
    private final String description;

    ModelType(String mlflowName, String description) {
        this.mlflowName = mlflowName;
        this.description = description;
    }

    public String getMlflowName() {
        return mlflowName;
    }

    public String getDescription() {
        return description;
    }
}
