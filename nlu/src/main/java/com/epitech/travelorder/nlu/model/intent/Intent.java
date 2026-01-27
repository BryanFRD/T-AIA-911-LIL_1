package com.epitech.travelorder.nlu.model.intent;

import com.epitech.travelorder.common.enumeration.IntentLabel;

public record Intent(
    IntentLabel intentLabel,
    float confidence
) {
    @Override
    public String toString() {
        return String.format("%s (%.2f%%)", intentLabel, confidence * 100);
    }
}
