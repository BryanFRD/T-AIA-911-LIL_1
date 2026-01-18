package com.epitech.travelorder.pathfinder.repository;

import com.epitech.travelorder.common.domain.sncf.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findAllByStopNameContainingIgnoreCase(String stopName);
}

