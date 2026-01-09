package com.epitech.travelorder.orchestrator.domain.exception.business;

import com.epitech.travelorder.orchestrator.domain.exception.TravelOrderException;

import static com.epitech.travelorder.orchestrator.infrastructure.config.enumeration.ReferentialIssue.NO_ORIGIN_DESTINATION_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class IncompleteTravelRequestException extends TravelOrderException {

    public IncompleteTravelRequestException(String message) {
        super(message, BAD_REQUEST, NO_ORIGIN_DESTINATION_FOUND);
    }
}
