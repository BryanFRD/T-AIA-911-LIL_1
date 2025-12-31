package com.epitech.travelorder.nlu.dto;

import com.epitech.travelorder.nlu.model.NerLabel;

public record NerEntity(
    String text,
    NerLabel label,
    float confidence,
    int start,
    int end
) {
    @Override
    public String toString() {
        return String.format("%s (%s, %.2f%%)", text, label.label(), confidence * 100);
    }
}

