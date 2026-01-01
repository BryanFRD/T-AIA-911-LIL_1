package com.epitech.travelorder.orchestrator.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record TranscribeRequest(
        @NotNull(message = "Audio file is required")
        @NotEmpty(message = "Audio file cannot be empty")
        MultipartFile audio
) {}

