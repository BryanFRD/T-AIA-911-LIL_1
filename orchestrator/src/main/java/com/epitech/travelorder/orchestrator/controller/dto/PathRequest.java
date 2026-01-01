package com.epitech.travelorder.orchestrator.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PathRequest(
        @NotBlank(message = "Customer path is required")
        String customerPath
) {}

