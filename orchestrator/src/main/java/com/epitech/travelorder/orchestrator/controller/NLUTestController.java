package com.epitech.travelorder.orchestrator.controller;

import com.epitech.travelorder.nlu.INLUService;
import com.epitech.travelorder.nlu.NLUResult;
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
    public ResponseEntity<NLUResult> predict(@RequestBody TextRequest request) {
        NLUResult result = nluService.predict(request.getText());
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("NLU service is running");
    }

    /**
     * Request DTO
     */
    public static class TextRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
