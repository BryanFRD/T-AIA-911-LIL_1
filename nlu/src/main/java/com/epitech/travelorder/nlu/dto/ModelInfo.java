package com.epitech.travelorder.nlu.dto;

public record ModelInfo(
    String name,
    String type,
    String version,
    String runId,
    boolean loaded
) {}

