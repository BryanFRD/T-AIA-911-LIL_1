package com.epitech.travelorder.pathfinder.service.rule;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.dto.sub.TravelLeg;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import com.epitech.travelorder.common.repository.StopRepository;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.mapper.TravelLegMapper;
import com.epitech.travelorder.pathfinder.service.MultiDaySearchService;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.TravelPathBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionJourneyExecutor implements RuleExecutor {

    private final LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "ConnectionJourneyExecutor");
    private final StopRepository stopRepository;
    private final MultiDaySearchService multiDaySearchService;


    @Override
    public boolean canExecute(final RouteQuery routeQuery) {
        return !routeQuery.hasVias() && routeQuery.hasOrigin() && routeQuery.hasDestination();
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public CustomerTravelSolution execute(final RouteQuery query) {
        log.info(LOG_CTX.format("Searching connection journey via Dijkstra (multi-day horizon) for %s -> %s",
                query.origin(), query.destination()));

        Set<Stop> stopsOrigin = stopRepository.findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(query.origin());
        Set<Stop> stopsDestination = stopRepository.findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(query.destination());

        if (stopsOrigin.isEmpty() || stopsDestination.isEmpty()) {
            log.warn(LOG_CTX.format("No stops found"));
            return CustomerTravelSolution.empty(query);
        }

        List<TravelPathBo> paths = multiDaySearchService.findPathsWithHorizon(
                stopsOrigin,
                stopsDestination,
                query.departureTime()
        );

        List<TravelProposal> proposals = paths.stream()
                .map(path -> toProposal(path, query.departureTime().toLocalDate()))
                .toList();

        log.info(LOG_CTX.format("Found %d connection proposals", proposals.size()));
        return new CustomerTravelSolution(query, proposals);
    }

    private TravelProposal toProposal(TravelPathBo path, LocalDate date) {
        List<TravelLeg> legs = path.segments().stream()
                .map(segment -> TravelLegMapper.INSTANCE.convert(segment, date))
                .toList();

        if (legs.isEmpty()) return null;

        TravelLeg first = legs.getFirst();
        TravelLeg last = legs.getLast();

        int hours = (int) (path.totalCost() / 60);
        int minutes = (int) (path.totalCost() % 60);

        return new TravelProposal(
                LocalDateTime.parse(first.departureTime()),
                LocalDateTime.parse(last.arrivalTime()),
                new TravelTime(hours, minutes),
                legs
        );
    }
}

