package com.epitech.travelorder.orchestrator.controller;

import com.epitech.travelorder.nlu.INLUService;
import com.epitech.travelorder.nlu.NLUResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nlu")
@RequiredArgsConstructor
public class NLUController {

    private final INLUService nluService;

    @PostMapping("/predict")
    public ResponseEntity<NLUResult> predict(@RequestBody String text) {
        NLUResult result = nluService.predict(text);
        return ResponseEntity.ok(result);
    }
}
