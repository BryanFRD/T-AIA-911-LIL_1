package com.epitech.travelorder.nlu.dto;

import com.epitech.travelorder.nlu.model.IntentLabel;

public record Intent(
    IntentLabel intentLabel,
    float confidence
) {
    @Override
    public String toString() {
        return String.format("%s (%.2f%%)", intentLabel, confidence * 100);
    }
}
