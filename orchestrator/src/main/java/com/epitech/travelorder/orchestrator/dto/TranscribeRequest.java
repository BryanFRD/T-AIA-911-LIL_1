package com.epitech.travelorder.orchestrator.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record TranscribeRequest(
        @NotNull(message = "Audio file is required")
        MultipartFile audio
) {
}
