package com.epitech.travelorder.common.domain.nlu.intent;

import com.epitech.travelorder.common.domain.enumeration.IntentLabel;

public record Intent(
    IntentLabel intentLabel,
    float confidence
) {
    @Override
    public String toString() {
        return String.format("%s (%.2f%%)", intentLabel, confidence * 100);
    }
}
