package com.epitech.travelorder.orchestrator.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerPathRequest(
        @NotBlank(message = "Customer path is required")
        String customerPath,
        Double latitude,
        Double longitude
) {}

