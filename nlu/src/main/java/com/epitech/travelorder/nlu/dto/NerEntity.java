package com.epitech.travelorder.nlu.dto;

import com.epitech.travelorder.nlu.model.NerLabel;

public record NerEntity(
    String text,
    NerLabel nerLabel,
    float confidence,
    int start,
    int end
) {
    @Override
    public String toString() {
        return String.format("%s (%s, %.2f%%)", text, nerLabel.label(), confidence * 100);
    }
}

