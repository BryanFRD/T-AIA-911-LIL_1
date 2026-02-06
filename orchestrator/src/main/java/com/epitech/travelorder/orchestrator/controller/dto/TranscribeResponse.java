package com.epitech.travelorder.orchestrator.controller.dto;

import com.epitech.travelorder.audio.bo.TranscriptionBo;

public record TranscribeResponse(
        TranscriptionBo transcription,
        boolean canSkipUserConfirmation
) {}

