package com.epitech.travelorder.pathfinder;

import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.rule.RuleExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Comparator.comparingInt;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathfindingRuleEngine {

    private final static LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "PathfindingRuleEngine");

    private final static int ONE_RULE_IS_RETURNED = 1;

    private final List<RuleExecutor> rules;

    public CustomerTravelSolution process(RouteQuery query) {
        List<RuleExecutor> ruleExecutors = rules.stream()
                .filter(rule -> rule.canExecute(query))
                .max(comparingInt(RuleExecutor::getPriority))
                .stream()
                .toList();

        if (isEmpty(ruleExecutors)) {
            log.error(LOG_CTX.format("No rule executor found for the given query: {}", query));
            return null;
        }

        if (size(ruleExecutors) > ONE_RULE_IS_RETURNED) {
            log.warn(LOG_CTX.format("Priority conflict: multiple rule executors found for the given query: {}. Executing the highest priority rule.", query));
        }

        return ruleExecutors
                .getFirst()
                .execute(query);
    }
}
