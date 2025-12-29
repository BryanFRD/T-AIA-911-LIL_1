package com.epitech.travelorder.nlu;

/**
 * NER labels in BIO (Begin-Inside-Outside) format
 */
public enum NERLabel {
    // Outside
    O("O"),

    // Location FROM (departure)
    B_FROM("B-FROM"),
    I_FROM("I-FROM"),

    // Location TO (arrival)
    B_TO("B-TO"),
    I_TO("I-TO"),

    // Location VIA (passage)
    B_VIA("B-VIA"),
    I_VIA("I-VIA"),

    // VOICE (person name)
    B_VOICE("B-VOICE"),
    I_VOICE("I-VOICE"),

    // TIME
    B_TIME("B-TIME"),
    I_TIME("I-TIME"),

    // DATE
    B_DATE("B-DATE"),
    I_DATE("I-DATE"),

    // TRANSPORT mode
    B_TRANSPORT("B-TRANSPORT"),
    I_TRANSPORT("I-TRANSPORT");

    private final String label;

    NERLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Check if this is a Begin tag
     */
    public boolean isBegin() {
        return label.startsWith("B-");
    }

    /**
     * Check if this is an Inside tag
     */
    public boolean isInside() {
        return label.startsWith("I-");
    }

    /**
     * Check if this is Outside
     */
    public boolean isOutside() {
        return this == O;
    }

    /**
     * Get the entity type (without B- or I- prefix)
     */
    public String getEntityType() {
        if (isOutside()) {
            return "O";
        }
        return label.substring(2); // Remove "B-" or "I-"
    }

    public static NERLabel fromString(String label) {
        for (NERLabel nerLabel : values()) {
            if (nerLabel.label.equalsIgnoreCase(label)) {
                return nerLabel;
            }
        }
        return O; // Default fallback
    }

    /**
     * Check if this label represents a FROM entity (B-FROM or I-FROM)
     */
    public boolean isFrom() {
        return this == B_FROM || this == I_FROM;
    }

    /**
     * Check if this label represents a TO entity (B-TO or I-TO)
     */
    public boolean isTo() {
        return this == B_TO || this == I_TO;
    }

    /**
     * Check if this label represents a VIA entity (B-VIA or I-VIA)
     */
    public boolean isVia() {
        return this == B_VIA || this == I_VIA;
    }

    /**
     * Check if this label represents a VOICE entity (B-VOICE or I-VOICE)
     */
    public boolean isVoice() {
        return this == B_VOICE || this == I_VOICE;
    }

    /**
     * Check if this label represents a TIME entity (B-TIME or I-TIME)
     */
    public boolean isTime() {
        return this == B_TIME || this == I_TIME;
    }

    /**
     * Check if this label represents a DATE entity (B-DATE or I-DATE)
     */
    public boolean isDate() {
        return this == B_DATE || this == I_DATE;
    }

    /**
     * Check if this label represents a TRANSPORT entity (B-TRANSPORT or I-TRANSPORT)
     */
    public boolean isTransport() {
        return this == B_TRANSPORT || this == I_TRANSPORT;
    }

    @Override
    public String toString() {
        return label;
    }
}
