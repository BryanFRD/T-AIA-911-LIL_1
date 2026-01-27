package com.epitech.travelorder.pathfinder.rule;

import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultiStopJourneyExecutor implements RuleExecutor {

    @Override
    public boolean canExecute(RouteQuery query) {
        return query.hasVias();
    }

    @Override
    public CustomerTravelSolution execute(RouteQuery query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
