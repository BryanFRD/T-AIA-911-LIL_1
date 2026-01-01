package com.epitech.travelorder.orchestrator.config.exception.business;

import com.epitech.travelorder.orchestrator.config.exception.TravelOrderException;

import static com.epitech.travelorder.orchestrator.config.enumeration.ReferentialIssue.NO_ORIGIN_DESTINATION_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class IncompleteTravelRequestException extends TravelOrderException {

    public IncompleteTravelRequestException(String message) {
        super(message, BAD_REQUEST, NO_ORIGIN_DESTINATION_FOUND);
    }
}
