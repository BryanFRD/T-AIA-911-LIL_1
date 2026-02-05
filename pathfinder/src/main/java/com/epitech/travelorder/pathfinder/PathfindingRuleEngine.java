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
        List<RuleExecutor> applicableRules = rules.stream()
                .filter(rule -> rule.canExecute(query))
                .sorted(comparingInt(RuleExecutor::getPriority))
                .toList();

        if (isEmpty(applicableRules)) {
            log.error(LOG_CTX.format("No rule executor found for the given query: %s", query));
            return CustomerTravelSolution.empty(query);
        }

        log.info(LOG_CTX.format("Found %d applicable rule(s) for query", size(applicableRules)));

        for (RuleExecutor rule : applicableRules) {
            log.debug(LOG_CTX.format("Executing rule: %s (priority: %d)",
                    rule.getClass().getSimpleName(), rule.getPriority()));

            CustomerTravelSolution result = rule.execute(query);

            if (result != null && !isEmpty(result.proposals())) {
                log.info(LOG_CTX.format("Rule %s found %d travel proposal(s)",
                        rule.getClass().getSimpleName(), size(result.proposals())));
                return result;
            }

            if (rule.shouldStopOnEmpty()) {
                log.debug(LOG_CTX.format("Rule %s returned empty and requested stop",
                        rule.getClass().getSimpleName()));
                break;
            }

            log.debug(LOG_CTX.format("Rule %s returned empty, trying next rule",
                    rule.getClass().getSimpleName()));
        }

        log.warn(LOG_CTX.format("No travel proposals found for query"));
        return CustomerTravelSolution.empty(query);
    }
}
