package com.epitech.travelorder.orchestrator.infrastructure.adapter.in.rest;

import com.epitech.travelorder.orchestrator.infrastructure.adapter.in.rest.dto.PathRequest;
import com.epitech.travelorder.orchestrator.infrastructure.adapter.in.rest.dto.TranscribeRequest;
import com.epitech.travelorder.orchestrator.infrastructure.adapter.in.rest.dto.TranscribeResponse;
import com.epitech.travelorder.orchestrator.domain.port.in.FindTravelPathUseCase;
import com.epitech.travelorder.orchestrator.domain.port.in.TranscribeAudioUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/travel-order")
@RequiredArgsConstructor
public class TravelOrderController {

    private final TranscribeAudioUseCase transcribeAudioUseCase;
    private final FindTravelPathUseCase findTravelPathUseCase;

    @PostMapping(value = "/transcribe", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscribeResponse> transcribeAudio(@Valid @ModelAttribute TranscribeRequest request) throws IOException {
        String transcribedText = transcribeAudioUseCase.transcribe(request.audio().getBytes());
        return ResponseEntity.ok(new TranscribeResponse(transcribedText));
    }

    @PostMapping("/find")
    public ResponseEntity<String> findPathForCustomer(@Valid @RequestBody PathRequest request) {
        String path = findTravelPathUseCase.findPath(request.customerPath());
        return ResponseEntity.ok(path);
    }
}