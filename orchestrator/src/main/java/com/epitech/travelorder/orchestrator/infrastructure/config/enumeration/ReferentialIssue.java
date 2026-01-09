package com.epitech.travelorder.orchestrator.infrastructure.config.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferentialIssue {

    NO_SOLUTION("ERR001", "No travel solution found."),
    NO_ORIGIN_DESTINATION_FOUND("ERR002", "Origin or destination not found."),
    AUDIO_PROCESSING_FAILED("ERR003", "Failed to process audio file."),
    INTERNAL_SERVICE_DOWN("ERR004", "An internal service is currently down.");

    private final String code;
    private final String description;
}
