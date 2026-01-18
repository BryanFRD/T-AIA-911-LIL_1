package com.epitech.travelorder.orchestrator.service;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.orchestrator.dto.TravelRequest;
import com.epitech.travelorder.pathfinder.service.PathfindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.epitech.travelorder.orchestrator.util.TravelRequestUtils.extract;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorPathfindingService {

    private final PathfindingService pathfinderService;

    private final OrchestratorNluService orchestratorNluService;

    public String findPath(String customerRequest) {
        Nlu nlu = orchestratorNluService.analyze(customerRequest);
        TravelRequest travelRequest = extract(nlu);
        if (isEmpty(travelRequest.viaPoints())) {
            return pathfinderService.findRoute(travelRequest.origin(), travelRequest.destination());
        }
        return pathfinderService.findRouteWithVia(travelRequest.origin(), travelRequest.destination(), travelRequest.viaPoints());
    }
}

