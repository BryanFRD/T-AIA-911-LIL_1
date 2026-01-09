package com.epitech.travelorder.orchestrator.domain.exception.technical;

import com.epitech.travelorder.orchestrator.domain.exception.TravelOrderException;

import static com.epitech.travelorder.orchestrator.infrastructure.config.enumeration.ReferentialIssue.AUDIO_PROCESSING_FAILED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class AudioProcessingException extends TravelOrderException {

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause, BAD_REQUEST, AUDIO_PROCESSING_FAILED);
    }
}

