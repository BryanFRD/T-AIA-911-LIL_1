package com.epitech.travelorder.nlu.dto;

public record NluResult(
    IntentResult intent,
    NerResult entities
) {

    @Override
    public String toString() {
        return String.format("NluResult{intent=%s, entities=%s}",
            intent,
            entities
        );
    }
}

