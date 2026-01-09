package com.epitech.travelorder.pathfinder.application.usecase;

import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.pathfinder.domain.port.in.FindRouteUseCase;
import com.epitech.travelorder.pathfinder.domain.port.out.StopRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class FindRouteUseCaseImpl implements FindRouteUseCase {

  private static final Log LOG_CTX = Log.of(Log.PATHFINDER, "FindRoute");

  private final StopRepositoryPort stopRepository;

  @Override
  public String findRoute(String origin, String destination) {
    log.info(LOG_CTX.format("Finding route from {} to {}", origin, destination));
    return String.format("Path from %s to %s", origin, destination);
  }

  @Override
  public String findRouteWithVia(String origin, String destination, List<String> viaPoints) {
    log.info(LOG_CTX.format("Finding route from {} to {} via {}", origin, destination, String.join(", ", viaPoints)));

    StringBuilder path = new StringBuilder();
    String currentOrigin = origin;

    for (String via : viaPoints) {
      path.append(String.format("Path from %s to %s\n", currentOrigin, via));
      currentOrigin = via;
    }

    path.append(String.format("Path from %s to %s", currentOrigin, destination));
    return path.toString();
  }
}

