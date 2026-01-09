package com.epitech.travelorder.common.domain.nlu.ner;

import com.epitech.travelorder.common.domain.enumeration.NerLabel;

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

