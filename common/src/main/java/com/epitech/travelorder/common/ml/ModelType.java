package com.epitech.travelorder.common.ml;

/**
 * Enumeration of available ML models with their MLflow identifiers and aliases
 */
public enum ModelType {
    NLU_INTENT("nlu-intent", "intent-classifier"),
    NLU_NER("nlu-ner", "named-entity-recognizer"),
    STT("stt", "speech-to-text"),
    TTS("tts", "text-to-speech");

    private final String mlflowName;
    private final String alias;

    ModelType(String mlflowName, String alias) {
        this.mlflowName = mlflowName;
        this.alias = alias;
    }

    public String getMlflowName() {
        return mlflowName;
    }

    public String getAlias() {
        return alias;
    }
}
