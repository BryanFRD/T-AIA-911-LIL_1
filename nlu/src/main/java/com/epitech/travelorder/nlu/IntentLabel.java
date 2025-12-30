package com.epitech.travelorder.nlu;

import lombok.Getter;
import lombok.ToString;

/**
 * Intent labels for classification
 */
@Getter
@ToString
public enum IntentLabel {
    TRAJET("trajet"),
    OTHER("other");

    private final String label;

    IntentLabel(String label) {
        this.label = label;
    }

    public static IntentLabel fromString(String label) {
        for (IntentLabel intent : values()) {
            if (intent.label.equalsIgnoreCase(label)) {
                return intent;
            }
        }
        return OTHER;
    }
}
