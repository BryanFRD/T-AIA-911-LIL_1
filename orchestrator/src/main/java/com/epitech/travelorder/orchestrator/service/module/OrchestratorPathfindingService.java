package com.epitech.travelorder.orchestrator.service.module;

import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import com.epitech.travelorder.orchestrator.controller.dto.CustomerPathRequest;
import com.epitech.travelorder.pathfinder.service.PathfindingRuleEngine;
import com.epitech.travelorder.pathfinder.service.RouteQueryEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorPathfindingService {

    private final RouteQueryEnricher routeQueryEnricher;
    private final PathfindingRuleEngine pathfindingRuleEngine;

    public CustomerTravelSolution findSolution(CustomerPathRequest customerRequest, RouteQuery routeQuery) {
        RouteQuery enrichedQuery = routeQueryEnricher.enrich(routeQuery, customerRequest.latitude(), customerRequest.longitude());

        if (!enrichedQuery.hasOrigin()) {
            log.error("Impossible to determine the origin for the RouteQuery: {}", enrichedQuery);
            // STOP the process if we cannot determine the origin
            throw new IncompleteTravelRequestException("Impossible to determine the origin.");
        }

        return pathfindingRuleEngine.process(enrichedQuery);
    }
}