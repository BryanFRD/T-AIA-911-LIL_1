package com.epitech.travelorder.orchestrator.service;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.orchestrator.controller.dto.CustomerPathRequest;
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

    public String transcribeAudio(byte[] audioData) {
        return orchestratorAudioService.transcribe(audioData);
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
