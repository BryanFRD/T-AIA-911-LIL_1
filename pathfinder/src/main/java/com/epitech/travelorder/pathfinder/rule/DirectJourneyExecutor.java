package com.epitech.travelorder.pathfinder.rule;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.common.repository.RouteRepository;
import com.epitech.travelorder.common.repository.StopRepository;
import com.epitech.travelorder.common.repository.StopTimeRepository;
import com.epitech.travelorder.common.repository.TripRepository;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.mapper.TravelProposalMapper;
import com.epitech.travelorder.pathfinder.util.bo.DirectTravelSolution;
import com.epitech.travelorder.pathfinder.service.TravelCalendarDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.epitech.travelorder.pathfinder.util.TimeUtils.convertLocalDateTimeToGtfsDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectJourneyExecutor implements RuleExecutor {

    private final LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "DirectJourneyExecutor");

    private final StopRepository stopRepository;

    private final RouteRepository routeRepository;

    private final TripRepository tripRepository;

    private final StopTimeRepository stopTimeRepository;

    private final TravelCalendarDateService travelCalendarDateService;

    @Override
    public boolean canExecute(final RouteQuery routeQuery) {
        return !routeQuery.hasVias() && routeQuery.hasOrigin() && routeQuery.hasDestination();
    }

    @Override
    public CustomerTravelSolution execute(final RouteQuery routeQuery) {
        Set<Stop> stopsOrigin = stopRepository.findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(routeQuery.origin());
        Set<Stop> stopsDestination = stopRepository.findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(routeQuery.destination());

        if (stopsOrigin.isEmpty() || stopsDestination.isEmpty()) {
            log.warn(LOG_CTX.format("No stops found for origin %s or destination %s", routeQuery.origin(), routeQuery.destination()));
            return CustomerTravelSolution.empty(routeQuery);
        }

        final String gtfsDate = convertLocalDateTimeToGtfsDate(routeQuery.departureTime());
        List<TravelProposal> travelProposals = new ArrayList<>();

        for (Stop stopOrigin : stopsOrigin) {
            Set<StopTime> starts = stopTimeRepository.findByStopIdAndDepartureTimeGreaterThanEqualOrderByDepartureTime(stopOrigin.getStopId(), gtfsDate);

            if (starts.isEmpty()) {
                continue;
            }

            for (StopTime startNode : starts) {
                String currentTripId = startNode.getTripId();

                for (Stop destination : stopsDestination) {
                    Optional<StopTime> endNodeOpt = stopTimeRepository.findByTripIdAndStopId(currentTripId, destination.getStopId());

                    if (endNodeOpt.isPresent()) {
                        StopTime endNode = endNodeOpt.get();

                        // Ensure that the endNode comes after the startNode in the trip sequence
                        if (endNode.getStopSequence() > startNode.getStopSequence()) {
                            Trip trip = tripRepository.findById(endNode.getTripId()).orElse(null);
                            if (trip == null) {
                                continue;
                            }

                            if (!travelCalendarDateService.canTravelForThisDate(trip.getServiceId(), routeQuery.departureTime().toLocalDate())) {
                                continue;
                            }

                            Route route = routeRepository.findById(trip.getRouteId()).orElse(null);
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
                                    routeQuery.departureTime().toLocalDate()
                            );

                            travelProposals.add(TravelProposalMapper.INSTANCE.convert(solutionBo));
                        }
                    }
                }
            }
        }
        return new CustomerTravelSolution(routeQuery, travelProposals);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
