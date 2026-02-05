package com.epitech.travelorder.pathfinder.service.algorithm.bo;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;

import java.time.LocalDate;

public record DirectTravelSolution(
        StopTime startStopTime,
        StopTime endStopTime,
        Trip trip,
        Route route,
        Stop origin,
        Stop destination,
        LocalDate referenceDate) {

}
