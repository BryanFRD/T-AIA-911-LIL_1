package com.epitech.travelorder.orchestrator.config.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.time.Instant.now;

@JsonInclude(NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(now(), status, error, message, path);
    }
}

