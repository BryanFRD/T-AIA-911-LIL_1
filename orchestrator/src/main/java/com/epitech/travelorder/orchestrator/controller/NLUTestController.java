package com.epitech.travelorder.orchestrator.controller;

import com.epitech.travelorder.nlu.INLUService;
import com.epitech.travelorder.nlu.NLUResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PreDestroy;

/**
 * REST Controller for NLU predictions (Hexagonal Architecture)
 */
@Slf4j
@RestController
@RequestMapping("/api/nlu")
@Tag(name = "NLU", description = "Natural Language Understanding API for intent classification and entity extraction")
public class NLUTestController {

    private final INLUService nluService;

    @Autowired
    public NLUTestController(INLUService nluService) {
        this.nluService = nluService;
        log.info("NLU Controller initialized");
    }

    @PreDestroy
    public void cleanup() {
        nluService.close();
    }

    /**
     * Perform NLU analysis (intent + entities)
     */
    @PostMapping("/predict")
    @Operation(
        summary = "Analyze text with NLU",
        description = "Performs intent classification and named entity recognition on the input text"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully analyzed text",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NLUResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content
        )
    })
    public ResponseEntity<NLUResult> predict(@RequestBody TextRequest request) {
        NLUResult result = nluService.predict(request.getText());
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the NLU service is running and get model information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.epitech.travelorder.nlu.NLUHealthStatus.class)
            )
        )
    })
    public ResponseEntity<com.epitech.travelorder.nlu.NLUHealthStatus> health() {
        return ResponseEntity.ok(nluService.getHealthStatus());
    }

    /**
     * Request DTO
     */
    @Setter
    @Getter
    @Schema(description = "Request body for NLU prediction")
    public static class TextRequest {
        @Schema(description = "Text to analyze", example = "Je veux aller à Paris demain à 14h")
        private String text;
    }
}
