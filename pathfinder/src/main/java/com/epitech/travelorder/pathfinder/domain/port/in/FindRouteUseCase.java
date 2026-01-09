package com.epitech.travelorder.pathfinder.domain.port.in;

import java.util.List;

public interface FindRouteUseCase {
  String findRoute(String origin, String destination);
  String findRouteWithVia(String origin, String destination, List<String> viaPoints);
}

