package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of intent classification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private String intent;
    private float confidence;

    @Override
    public String toString() {
        return String.format("Intent: %s (confidence: %.2f%%)", intent, confidence * 100);
    }
}
