package com.epitech.travelorder.orchestrator.config.exception.technical;

import com.epitech.travelorder.orchestrator.config.exception.TravelOrderException;

import static com.epitech.travelorder.orchestrator.config.enumeration.ReferentialIssue.INTERNAL_SERVICE_DOWN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

public class InternalServiceDown extends TravelOrderException {

    public InternalServiceDown(String message) {
        super(message, SERVICE_UNAVAILABLE, INTERNAL_SERVICE_DOWN);
    }
}
