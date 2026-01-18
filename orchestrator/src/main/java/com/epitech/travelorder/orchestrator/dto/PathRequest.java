package com.epitech.travelorder.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

public record PathRequest(
        @NotBlank(message = "Customer path is required")
        String customerPath
) {}

