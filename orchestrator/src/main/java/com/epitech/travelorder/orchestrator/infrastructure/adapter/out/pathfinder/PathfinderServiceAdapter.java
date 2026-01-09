package com.epitech.travelorder.orchestrator.infrastructure.adapter.out.pathfinder;

import com.epitech.travelorder.orchestrator.domain.port.out.PathfinderPort;
import com.epitech.travelorder.pathfinder.PathfinderService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PathfinderServiceAdapter implements PathfinderPort {

    private final PathfinderService pathfinderService;

    @Override
    public String findRoute(String origin, String destination) {
        return pathfinderService.findRoute(origin, destination);
    }

    @Override
    public String findRouteWithVia(String origin, String destination, List<String> viaPoints) {
        return pathfinderService.findRouteWithVia(origin, destination, viaPoints);
    }
}

