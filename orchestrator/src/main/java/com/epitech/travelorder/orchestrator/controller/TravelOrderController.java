package com.epitech.travelorder.orchestrator.controller;

import com.epitech.travelorder.orchestrator.controller.dto.PathRequest;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeRequest;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeResponse;
import com.epitech.travelorder.orchestrator.service.TravelOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/travel-order")
@RequiredArgsConstructor
public class TravelOrderController {

    private final TravelOrderService travelOrderService;

    @PostMapping(value = "/transcribe", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscribeResponse> transcribeAudio(@Valid @ModelAttribute TranscribeRequest request) {
        return ResponseEntity.ok(travelOrderService.transcribeAudioFromCustomerFile(request));
    }

    @PostMapping("/find")
    public ResponseEntity<String> findPathForCustomer(@Valid @RequestBody PathRequest request) {
        return ResponseEntity.ok(travelOrderService.processCustomerRequest(request));
    }
}