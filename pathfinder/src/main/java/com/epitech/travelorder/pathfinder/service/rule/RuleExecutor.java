package com.epitech.travelorder.pathfinder.service.rule;

import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;

public interface RuleExecutor {
    boolean canExecute(RouteQuery routeQuery);
    CustomerTravelSolution execute(RouteQuery routeQuery);
    int getPriority();
}
