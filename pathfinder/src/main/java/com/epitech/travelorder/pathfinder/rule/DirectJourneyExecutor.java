package com.epitech.travelorder.pathfinder.rule;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.config.PathfinderSearchProperties;
import com.epitech.travelorder.pathfinder.mapper.TravelProposalMapper;
import com.epitech.travelorder.pathfinder.service.CachedDataService;
import com.epitech.travelorder.pathfinder.service.TravelCalendarDateService;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.DirectTravelSolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toGtfsTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectJourneyExecutor implements RuleExecutor {

    private final LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "DirectJourneyExecutor");
    private final PathfinderSearchProperties searchProperties;
    private final TravelCalendarDateService travelCalendarDateService;
    private final CachedDataService cachedDataService;

    @Override
    public boolean canExecute(final RouteQuery routeQuery) {
        return !routeQuery.hasVias() && routeQuery.hasOrigin() && routeQuery.hasDestination();
    }

    @Override
    public CustomerTravelSolution execute(final RouteQuery routeQuery) {
        log.info(LOG_CTX.format("Searching direct journey from %s to %s (multi-day horizon: %d days)",
                routeQuery.origin(), routeQuery.destination(), searchProperties.getSearchHorizonDays()));

        Set<Stop> stopsOrigin = cachedDataService.findStopsByName(routeQuery.origin());
        Set<Stop> stopsDestination = cachedDataService.findStopsByName(routeQuery.destination());

        if (stopsOrigin.isEmpty() || stopsDestination.isEmpty()) {
            log.warn(LOG_CTX.format("No stops found for origin %s or destination %s", routeQuery.origin(), routeQuery.destination()));
            return CustomerTravelSolution.empty(routeQuery);
        }

        List<TravelProposal> allProposals = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < searchProperties.getSearchHorizonDays(); dayOffset++) {
            LocalDate searchDate = routeQuery.departureTime().toLocalDate().plusDays(dayOffset);
            String gtfsTime = dayOffset == 0
                    ? toGtfsTime(routeQuery.departureTime())
                    : "00:00:00";

            log.debug(LOG_CTX.format("Searching on day offset %d (%s) with time %s", dayOffset, searchDate, gtfsTime));

            List<TravelProposal> dayProposals = searchDirectJourneysForDay(
                    stopsOrigin,
                    stopsDestination,
                    gtfsTime,
                    searchDate,
                    routeQuery
            );

            allProposals.addAll(dayProposals);

            if (allProposals.size() >= searchProperties.getMaxResults()) {
                break;
            }
        }

        List<TravelProposal> limitedProposals = allProposals.stream()
                .sorted(Comparator.comparing(TravelProposal::departureTime))
                .limit(searchProperties.getMaxResults())
                .toList();

        log.info(LOG_CTX.format("Found %d direct journey proposals", limitedProposals.size()));
        return new CustomerTravelSolution(routeQuery, limitedProposals);
    }

    private List<TravelProposal> searchDirectJourneysForDay(
            Set<Stop> stopsOrigin,
            Set<Stop> stopsDestination,
            String gtfsTime,
            LocalDate searchDate,
            RouteQuery routeQuery
    ) {
        List<TravelProposal> proposals = new ArrayList<>();

        for (Stop stopOrigin : stopsOrigin) {
            Set<StopTime> starts = cachedDataService.findDeparturesByStopAndTime(stopOrigin.getStopId(), gtfsTime);

            if (starts.isEmpty()) {
                continue;
            }

            for (StopTime startNode : starts) {
                String currentTripId = startNode.getTripId();

                for (Stop destination : stopsDestination) {
                    Optional<StopTime> endNodeOpt = cachedDataService.findStopTimeByTripAndStop(currentTripId, destination.getStopId());

                    if (endNodeOpt.isEmpty()) {
                        continue;
                    }

                    StopTime endNode = endNodeOpt.get();

                    if (endNode.getStopSequence() <= startNode.getStopSequence()) {
                        continue;
                    }

                    Trip trip = cachedDataService.findTripById(currentTripId).orElse(null);

                    if (trip == null) {
                        continue;
                    }

                    if (!travelCalendarDateService.canTravelForThisDate(trip.getServiceId(), searchDate)) {
                        continue;
                    }

                    Route route = cachedDataService.findRouteById(trip.getRouteId())
                            .orElse(null);

                    if (route == null) {
                        continue;
                    }

                    DirectTravelSolution solutionBo = new DirectTravelSolution(
                            startNode,
                            endNode,
                            trip,
                            route,
                            stopOrigin,
                            destination,
                            searchDate
                    );

                    proposals.add(TravelProposalMapper.INSTANCE.convert(solutionBo));

                    if (proposals.size() >= searchProperties.getMaxResults()) {
                        return proposals;
                    }
                }
            }
        }

        return proposals;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean shouldStopOnEmpty() {
        return false;
    }
}
