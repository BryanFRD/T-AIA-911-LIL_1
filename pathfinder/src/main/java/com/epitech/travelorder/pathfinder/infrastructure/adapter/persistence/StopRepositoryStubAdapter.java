package com.epitech.travelorder.pathfinder.infrastructure.adapter.persistence;

import com.epitech.travelorder.common.domain.gtfs.Stop;
import com.epitech.travelorder.pathfinder.domain.port.out.StopRepositoryPort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StopRepositoryStubAdapter implements StopRepositoryPort {

  @Override
  public Optional<Stop> findByName(String name) {
    return Optional.empty();
  }

  @Override
  public List<Stop> findAll() {
    return Collections.emptyList();
  }
}

