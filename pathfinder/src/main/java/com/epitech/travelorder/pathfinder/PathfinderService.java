package com.epitech.travelorder.pathfinder;

import com.epitech.travelorder.pathfinder.application.usecase.FindRouteUseCaseImpl;
import com.epitech.travelorder.pathfinder.domain.port.in.FindRouteUseCase;
import com.epitech.travelorder.pathfinder.domain.port.out.StopRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PathfinderService {

  private final FindRouteUseCase findRouteUseCase;

  public PathfinderService(StopRepositoryPort stopRepository) {
    this.findRouteUseCase = new FindRouteUseCaseImpl(stopRepository);
  }

  public String findRoute(String origin, String destination) {
    return findRouteUseCase.findRoute(origin, destination);
  }

  public String findRouteWithVia(String origin, String destination, List<String> viaPoints) {
    return findRouteUseCase.findRouteWithVia(origin, destination, viaPoints);
  }
}

