package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, String> {

    Optional<Trip> findByRoute(Route route);

}
