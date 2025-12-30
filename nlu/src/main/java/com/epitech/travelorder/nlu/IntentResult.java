package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Result of intent classification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IntentResult {
    private IntentLabel intent;
    private float confidence;
}
