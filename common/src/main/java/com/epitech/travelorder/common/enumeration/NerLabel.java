package com.epitech.travelorder.common.enumeration;

public enum NerLabel {
    O("O"),
    B_FROM("B-FROM"),
    I_FROM("I-FROM"),
    B_TO("B-TO"),
    I_TO("I-TO"),
    B_VIA("B-VIA"),
    I_VIA("I-VIA"),
    B_VOICE("B-VOICE"),
    I_VOICE("I-VOICE"),
    B_TIME("B-TIME"),
    I_TIME("I-TIME"),
    B_DATE("B-DATE"),
    I_DATE("I-DATE"),
    B_TRANSPORT("B-TRANSPORT"),
    I_TRANSPORT("I-TRANSPORT");

    private final String label;

    NerLabel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isBegin() {
        return label.startsWith("B-");
    }

    public boolean isInside() {
        return label.startsWith("I-");
    }

    public boolean isOutside() {
        return this == O;
    }

    public String entityType() {
        if (isOutside()) return "O";
        return label.substring(2);
    }

    public static NerLabel fromString(String label) {
        if (label == null) return O;

        for (NerLabel nerLabel : values()) {
            if (nerLabel.label.equalsIgnoreCase(label)) {
                return nerLabel;
            }
        }
        return O;
    }

    public boolean isFrom() {
        return this == B_FROM || this == I_FROM;
    }

    public boolean isTo() {
        return this == B_TO || this == I_TO;
    }

    public boolean isVia() {
        return this == B_VIA || this == I_VIA;
    }

    public boolean isVoice() {
        return this == B_VOICE || this == I_VOICE;
    }

    public boolean isTime() {
        return this == B_TIME || this == I_TIME;
    }

    public boolean isDate() {
        return this == B_DATE || this == I_DATE;
    }

    public boolean isTransport() {
        return this == B_TRANSPORT || this == I_TRANSPORT;
    }
}

