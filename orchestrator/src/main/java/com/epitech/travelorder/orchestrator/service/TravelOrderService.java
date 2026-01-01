package com.epitech.travelorder.orchestrator.service;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.util.LogContext;
import com.epitech.travelorder.nlu.dto.NerEntity;
import com.epitech.travelorder.nlu.dto.Nlu;
import com.epitech.travelorder.nlu.model.NerLabel;
import com.epitech.travelorder.nlu.service.NluPredictService;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import com.epitech.travelorder.orchestrator.config.exception.technical.AudioProcessingException;
import com.epitech.travelorder.orchestrator.config.exception.technical.InternalServiceDown;
import com.epitech.travelorder.orchestrator.controller.dto.PathRequest;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeRequest;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeResponse;
import com.epitech.travelorder.pathfinder.PathfinderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.epitech.travelorder.nlu.model.NerLabel.B_FROM;
import static com.epitech.travelorder.nlu.model.NerLabel.B_TO;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelOrderService {

    private static final LogContext LOG_CTX = LogContext.of(LogContext.ORCH, "OrchestratorService");

    private final AudioService audioService;
    private final NluPredictService nluService;
    private final PathfinderService pathfinderService;

    public TranscribeResponse transcribeAudioFromCustomerFile(TranscribeRequest request) {
        try {
            MultipartFile audioFile = request.audio();
            String transcribedText = audioService.speechToText(audioFile.getBytes());
            return new TranscribeResponse(transcribedText);
        } catch (IOException e) {
            log.error(LOG_CTX.format("Failed to read audio file"), e);
            throw new AudioProcessingException("Invalid or corrupted audio file", e);
        }
    }

    public String processCustomerRequest(PathRequest request) {
        log.info(LOG_CTX.format("Processing customer request: {}", request.customerPath()));

        Nlu nlu = ofNullable(nluService.predict(request.customerPath()))
                .orElseThrow(() -> new InternalServiceDown("NLU service returned no response"));

        List<NerEntity> entities = (nlu.ner() != null) ? emptyIfNull(nlu.ner().entities()) : List.of();

        String origin = extractEntityText(entities, B_FROM)
                .orElseThrow(() -> new IncompleteTravelRequestException("Origin (FROM) missing in request"));

        String destination = extractEntityText(entities, B_TO)
                .orElseThrow(() -> new IncompleteTravelRequestException("Destination (TO) missing in request"));

        log.info(LOG_CTX.format("Extracted origin: {}, destination: {}", origin, destination));

        // TODO: Call pathfinderService
        // return pathfinderService.findPath(origin, destination);

        return String.format("Path from %s to %s", origin, destination);
    }

    private Optional<String> extractEntityText(List<NerEntity> entities, NerLabel targetLabel) {
        return entities.stream()
                .filter(nerEntity -> targetLabel.equals(nerEntity.nerLabel()))
                .findFirst()
                .map(NerEntity::text);
    }
}