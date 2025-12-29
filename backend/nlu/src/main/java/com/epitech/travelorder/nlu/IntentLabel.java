package com.epitech.travelorder.nlu;

/**
 * Intent labels for classification
 */
public enum IntentLabel {
    TRAJET("trajet"),
    OTHER("other");

    private final String label;

    IntentLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static IntentLabel fromString(String label) {
        for (IntentLabel intent : values()) {
            if (intent.label.equalsIgnoreCase(label)) {
                return intent;
            }
        }
        return OTHER; // Default fallback
    }

    @Override
    public String toString() {
        return label;
    }
}
