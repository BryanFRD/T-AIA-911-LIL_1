package com.epitech.travelorder.nlu.model;

public record NluModelInfo(
    String name,
    String type,
    String version,
    String runId,
    boolean loaded
) {}

