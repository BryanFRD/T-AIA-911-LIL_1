package com.epitech.travelorder.orchestrator.service;

import com.epitech.travelorder.audio.bo.TranscriptionBo;
import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.orchestrator.controller.dto.CustomerPathRequest;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeResponse;
import com.epitech.travelorder.orchestrator.mapper.TravelOrderResponseMapper;
import com.epitech.travelorder.orchestrator.mapper.TravelRequestMapper;
import com.epitech.travelorder.orchestrator.service.module.OrchestratorAudioService;
import com.epitech.travelorder.orchestrator.service.module.OrchestratorNluService;
import com.epitech.travelorder.orchestrator.service.module.OrchestratorPathfindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final OrchestratorAudioService orchestratorAudioService;
    private final OrchestratorNluService orchestratorNluService;
    private final OrchestratorPathfindingService orchestratorPathfindingService;

    public TranscribeResponse transcribeAudio(byte[] audioData) {
        TranscriptionBo transcription = orchestratorAudioService.transcribe(audioData);
        return TravelOrderResponseMapper.INSTANCE.convert(transcription);
    }

    public CustomerTravelSolution findCustomerSolution(CustomerPathRequest customerRequest) {
        // 1. Use NLU to understand the customer request
        final Nlu nlu = orchestratorNluService.getUnderstandingCustomerPath(customerRequest.customerPath());

        // 2. Map NLU to RouteQuery
        final RouteQuery routeQuery = TravelRequestMapper.INSTANCE.convert(nlu);

        // 3. Find the best travel solution
        return orchestratorPathfindingService.findSolution(customerRequest, routeQuery);
    }
}
