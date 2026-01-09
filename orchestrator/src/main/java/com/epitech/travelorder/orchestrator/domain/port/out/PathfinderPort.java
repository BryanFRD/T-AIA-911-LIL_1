package com.epitech.travelorder.orchestrator.domain.port.out;

import java.util.List;

public interface PathfinderPort {
    String findRoute(String origin, String destination);
    String findRouteWithVia(String origin, String destination, List<String> viaPoints);
}

