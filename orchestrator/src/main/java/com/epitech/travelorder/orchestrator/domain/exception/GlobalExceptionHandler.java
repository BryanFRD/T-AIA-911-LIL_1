package com.epitech.travelorder.orchestrator.domain.exception;

import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.orchestrator.domain.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Log LOG_CTX = Log.of(Log.ORCH, "GlobalExceptionHandler");

    @ExceptionHandler(TravelOrderException.class)
    public ResponseEntity<ErrorResponse> handleTravelOrderException(TravelOrderException e, HttpServletRequest request) {
        log.error(LOG_CTX.format("TravelOrderException occurred: %s", e.getMessage()), e);
        ErrorResponse error = ErrorResponse.of(
                e.getStatus().value(),
                e.getReferentialIssue().getCode(),
                e.getReferentialIssue().getDescription(),
                request.getRequestURI()
        );
        return ResponseEntity.status(e.getStatus()).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.error(LOG_CTX.format("File too large"), e);
        ErrorResponse error = ErrorResponse.of(
                PAYLOAD_TOO_LARGE.value(),
                "Payload Too Large",
                "File size exceeds maximum allowed",
                request.getRequestURI()
        );
        return ResponseEntity.status(PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Invalid argument: %s", e.getMessage()));
        ErrorResponse error = ErrorResponse.of(
                BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn(LOG_CTX.format("Validation failed: %s", errorMessage));
        ErrorResponse error = ErrorResponse.of(
                BAD_REQUEST.value(),
                "Bad Request",
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Resource not found: %s", e.getMessage()));
        ErrorResponse error = ErrorResponse.of(
                NOT_FOUND.value(),
                "Not Found",
                "Resource not found",
                request.getRequestURI()
        );
        return ResponseEntity.status(NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Method not allowed: %s", e.getMethod()));
        ErrorResponse error = ErrorResponse.of(
                METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                "HTTP method not supported: " + e.getMethod(),
                request.getRequestURI()
        );
        return ResponseEntity.status(METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Unsupported media type: %s", e.getContentType()));
        ErrorResponse error = ErrorResponse.of(
                UNSUPPORTED_MEDIA_TYPE.value(),
                "Unsupported Media Type",
                "Content type not supported",
                request.getRequestURI()
        );
        return ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Missing parameter: %s", e.getParameterName()));
        ErrorResponse error = ErrorResponse.of(
                BAD_REQUEST.value(),
                "Bad Request",
                "Missing required parameter: " + e.getParameterName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Missing request part: %s", e.getRequestPartName()));
        ErrorResponse error = ErrorResponse.of(
                BAD_REQUEST.value(),
                "Bad Request",
                "Missing required file: " + e.getRequestPartName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn(LOG_CTX.format("Invalid request body"), e);
        ErrorResponse error = ErrorResponse.of(
                BAD_REQUEST.value(),
                "Bad Request",
                "Invalid request format",
                request.getRequestURI()
        );
        return ResponseEntity.status(BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
        log.error(LOG_CTX.format("Unexpected error"), e);
        ErrorResponse error = ErrorResponse.of(
                INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An internal error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(error);
    }
}

