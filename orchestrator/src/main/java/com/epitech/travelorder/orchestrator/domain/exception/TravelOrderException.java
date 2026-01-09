package com.epitech.travelorder.orchestrator.domain.exception;

import com.epitech.travelorder.orchestrator.infrastructure.config.enumeration.ReferentialIssue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Getter
public class TravelOrderException extends RuntimeException {

    private final HttpStatus status;
    private final ReferentialIssue referentialIssue;

    public TravelOrderException(HttpStatus status, ReferentialIssue referentialIssue) {
        super();
        this.status = status;
        this.referentialIssue = referentialIssue;
    }

    public TravelOrderException(String message, HttpStatus status, ReferentialIssue referentialIssue) {
        super(message);
        this.status = status;
        this.referentialIssue = referentialIssue;
    }

    public TravelOrderException(String message, Throwable cause, HttpStatus status, ReferentialIssue referentialIssue) {
        super(message, cause);
        this.status = status;
        this.referentialIssue = referentialIssue;
    }
}