package com.epitech.travelorder.nlu.dto;

import com.epitech.travelorder.nlu.model.IntentLabel;

public record IntentResult (
    IntentLabel intent,
    float confidence
) {
    @Override
    public String toString() {
        return String.format("%s (%.2f%%)", intent.label(), confidence * 100);
    }
}
