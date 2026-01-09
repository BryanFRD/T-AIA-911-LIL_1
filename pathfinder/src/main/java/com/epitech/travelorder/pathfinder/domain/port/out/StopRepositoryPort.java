package com.epitech.travelorder.pathfinder.domain.port.out;

import com.epitech.travelorder.common.domain.gtfs.Stop;

import java.util.List;
import java.util.Optional;

public interface StopRepositoryPort {
  Optional<Stop> findByName(String name);
  List<Stop> findAll();
}

