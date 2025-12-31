package com.epitech.travelorder.orchestrator.controller;

import com.epitech.travelorder.common.util.LogContext;
import com.epitech.travelorder.nlu.dto.NluResult;
import com.epitech.travelorder.nlu.service.NluPredictService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/nlu")
@RequiredArgsConstructor
public class NluController {

    private static final LogContext LOG_CTX = LogContext.of(LogContext.ORCH, "NLUController");

    private final NluPredictService nluPredictService;

    @PostMapping("/predict")
    public ResponseEntity<NluResult> predict(@RequestBody @NotNull String text) {
        log.info(LOG_CTX.format("Received prediction request"));

        NluResult result = nluPredictService.predict(text);

        log.info(LOG_CTX.format("Prediction completed successfully"));
        return ResponseEntity.ok(result);
    }
}

