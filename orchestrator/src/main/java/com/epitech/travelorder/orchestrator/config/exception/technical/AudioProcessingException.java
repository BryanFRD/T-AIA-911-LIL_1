package com.epitech.travelorder.orchestrator.config.exception.technical;

import com.epitech.travelorder.orchestrator.config.exception.TravelOrderException;

import static com.epitech.travelorder.orchestrator.config.enumeration.ReferentialIssue.AUDIO_PROCESSING_FAILED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class AudioProcessingException extends TravelOrderException {

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause, BAD_REQUEST, AUDIO_PROCESSING_FAILED);
    }
}

