package com.epitech.travelorder.pathfinder.service.algorithm.bo;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import lombok.Builder;

@Builder
public record GraphSegmentBo(
        StopTime startStopTime,
        StopTime endStopTime,
        Stop startStop,
        Stop endStop,
        Trip trip,
        Route route,
        double cost,
        boolean isTransfer
) {}