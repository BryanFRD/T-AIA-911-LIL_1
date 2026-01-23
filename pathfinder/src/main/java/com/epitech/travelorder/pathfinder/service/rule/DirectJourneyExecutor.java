package com.epitech.travelorder.pathfinder.service.rule;

import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;

public class DirectJourneyExecutor implements RuleExecutor {

    @Override
    public boolean canExecute(final RouteQuery routeQuery) {
        return !routeQuery.hasVias() && routeQuery.hasOrigin();
    }

    @Override
    public CustomerTravelSolution execute(final RouteQuery routeQuery) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
