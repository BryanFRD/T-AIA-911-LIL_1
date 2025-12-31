package com.epitech.travelorder.nlu.model;


public enum IntentLabel {
    TRAJET("TRAJET"),
    OTHER("OTHER");

    private final String label;

    IntentLabel(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
