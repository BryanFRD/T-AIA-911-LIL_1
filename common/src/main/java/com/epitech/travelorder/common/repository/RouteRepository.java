package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {

    Set<Route> findAllByRouteLongNameContainingIgnoreCase(String routeLongName);
}
