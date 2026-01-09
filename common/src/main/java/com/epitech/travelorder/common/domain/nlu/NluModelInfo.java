package com.epitech.travelorder.common.domain.nlu;

public record NluModelInfo(
    String name,
    String type,
    String version,
    String runId,
    boolean loaded
) {}

