package com.epitech.travelorder.pathfinder.rule;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.dto.CustomerTravelSolution;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.dto.sub.TravelLeg;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.config.PathfinderSearchProperties;
import com.epitech.travelorder.pathfinder.mapper.TravelLegMapper;
import com.epitech.travelorder.pathfinder.service.CaffeineCacheService;
import com.epitech.travelorder.pathfinder.service.MultiDaySearchService;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.GraphSegmentBo;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.TravelPathBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toLocalDateTime;
import static java.time.Duration.between;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiStopJourneyExecutor implements RuleExecutor {

    private final LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "MultiStopJourneyExecutor");
    private final PathfinderSearchProperties searchProperties;
    private final CaffeineCacheService caffeineCacheService;
    private final MultiDaySearchService multiDaySearchService;

    @Override
    public boolean canExecute(RouteQuery query) {
        return query.hasVias();
    }

    @Override
    public CustomerTravelSolution execute(RouteQuery query) {
        log.info(LOG_CTX.format("Searching multi-stop journey with imposed vias: %s (multi-day horizon: %d days)",
                query.vias(), searchProperties.getSearchHorizonDays()));

        List<String> waypoints = new ArrayList<>();
        waypoints.add(query.origin());
        waypoints.addAll(query.vias());
        waypoints.add(query.destination());

        List<TravelLeg> allLegs = new ArrayList<>();
        LocalDateTime currentDepartureTime = query.departureTime();

        for (int i = 0; i < waypoints.size() - 1; i++) {
            String from = waypoints.get(i);
            String to = waypoints.get(i + 1);

            log.debug(LOG_CTX.format("Searching segment %d/%d: %s -> %s", i + 1, waypoints.size() - 1, from, to));

            Set<Stop> fromStops = caffeineCacheService.findStopsByName(from);
            Set<Stop> toStops = caffeineCacheService.findStopsByName(to);

            if (fromStops.isEmpty() || toStops.isEmpty()) {
                log.warn(LOG_CTX.format("No stops found for segment %s -> %s", from, to));
                return CustomerTravelSolution.empty(query);
            }

            List<TravelPathBo> paths = multiDaySearchService.findPathsWithHorizon(
                    fromStops,
                    toStops,
                    currentDepartureTime
            );

            if (paths.isEmpty()) {
                log.warn(LOG_CTX.format("No path found for segment %s -> %s", from, to));
                return CustomerTravelSolution.empty(query);
            }

            TravelPathBo bestPath = paths.getFirst();

            log.info(LOG_CTX.format("Found path for segment %s -> %s with %d legs, cost: %.2f minutes",
                    from, to, bestPath.segments().size(), bestPath.totalCost()));

            // Convert each segment with the correct date
            LocalDateTime segmentStartTime = currentDepartureTime;
            for (GraphSegmentBo segment : bestPath.segments()) {
                // Calculate the actual date for this segment's departure
                LocalDateTime segmentDeparture = toLocalDateTime(
                        segment.startStopTime().getDepartureTime(),
                        segmentStartTime.toLocalDate()
                );

                // If departure time is before the expected start time, it must be the next day
                if (segmentDeparture.isBefore(segmentStartTime)) {
                    log.debug(LOG_CTX.format("Day overflow detected: departure %s is before expected %s, adding 1 day",
                            segmentDeparture, segmentStartTime));
                    segmentDeparture = toLocalDateTime(
                            segment.startStopTime().getDepartureTime(),
                            segmentStartTime.toLocalDate().plusDays(1)
                    );
                }

                TravelLeg leg = TravelLegMapper.INSTANCE.convert(segment, segmentDeparture.toLocalDate());
                allLegs.add(leg);

                log.debug(LOG_CTX.format("Leg: %s (%s) -> %s (%s), route: %s",
                        segment.startStop().getStopName(), leg.departureTime(),
                        segment.endStop().getStopName(), leg.arrivalTime(),
                        segment.route().getRouteShortName()));

                // Update segment start time for next iteration
                segmentStartTime = toLocalDateTime(
                        segment.endStopTime().getArrivalTime(),
                        segmentDeparture.toLocalDate()
                );

                // Handle day overflow for arrival time
                if (segmentStartTime.isBefore(segmentDeparture)) {
                    segmentStartTime = toLocalDateTime(
                            segment.endStopTime().getArrivalTime(),
                            segmentDeparture.toLocalDate().plusDays(1)
                    );
                }
            }

            // Set next departure time with transfer buffer
            currentDepartureTime = segmentStartTime.plusMinutes(10);
        }

        if (allLegs.isEmpty()) {
            log.warn(LOG_CTX.format("No legs found for multi-stop journey"));
            return CustomerTravelSolution.empty(query);
        }

        LocalDateTime finalDeparture = LocalDateTime.parse(allLegs.getFirst().departureTime());
        LocalDateTime finalArrival = LocalDateTime.parse(allLegs.getLast().arrivalTime());

        // Validate that arrival is after departure
        if (finalArrival.isBefore(finalDeparture)) {
            log.error(LOG_CTX.format("Invalid journey: arrival time %s is before departure time %s",
                    finalArrival, finalDeparture));
            return CustomerTravelSolution.empty(query);
        }

        long totalMinutes = between(finalDeparture, finalArrival).toMinutes();
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);

        log.info(LOG_CTX.format("Multi-stop journey: departure=%s, arrival=%s, duration=%dh%dm, legs=%d",
                finalDeparture, finalArrival, hours, minutes, allLegs.size()));

        TravelProposal proposal = new TravelProposal(
                finalDeparture,
                finalArrival,
                new TravelTime(hours, minutes),
                allLegs
        );

        log.info(LOG_CTX.format("Found multi-stop journey with %d legs", allLegs.size()));
        return new CustomerTravelSolution(query, List.of(proposal));
    }

    @Override
    public int getPriority() {
        return 3;
    }
}

