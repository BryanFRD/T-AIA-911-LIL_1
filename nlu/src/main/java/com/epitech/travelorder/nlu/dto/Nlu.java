package com.epitech.travelorder.nlu.dto;

public record Nlu(
    Intent intent,
    Ner ner
) {

    @Override
    public String toString() {
        return String.format("NluResult{intentLabel=%s, entities=%s}",
            intent,
            ner
        );
    }
}

