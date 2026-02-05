package com.epitech.travelorder.pathfinder.service.algorithm;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.pathfinder.config.PathfinderSearchProperties;
import com.epitech.travelorder.pathfinder.service.CachedDataService;
import com.epitech.travelorder.pathfinder.service.TravelCalendarDateService;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.GraphSegmentBo;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.PathNode;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.TravelPathBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toGtfsTime;
import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toLocalDateTime;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class DijkstraPathfinderService {

    private static final int TRANSFER_BUFFER_MINUTES = 10;
    private static final int MAX_SEARCH_HOURS = 24;
    private static final int MAX_ITERATIONS = 100000;
    private static final int MAX_DEPARTURES_PER_STOP = 50;

    private final PathfinderSearchProperties searchProperties;
    private final CachedDataService cachedDataService;
    private final TravelCalendarDateService calendarService;

    public List<TravelPathBo> findPaths(Set<Stop> origins, Set<Stop> destinations, String gtfsStartTime, LocalDate date) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<String, Double> bestTimes = new HashMap<>();
        Set<String> visitedNodes = new HashSet<>();
        List<TravelPathBo> foundPaths = new ArrayList<>();

        Set<String> destinationIds = destinations.stream()
                .map(Stop::getStopId)
                .collect(toSet());

        LocalDateTime queryBaseTime = toLocalDateTime(gtfsStartTime, date);
        LocalDateTime maxSearchTime = queryBaseTime.plusHours(MAX_SEARCH_HOURS);

        // Initialize with all origin stops
        for (Stop origin : origins) {
            PathNode startNode = PathNode.builder()
                    .stop(origin)
                    .costFromStart(0.0)
                    .transferCount(0)
                    .build();
            openSet.add(startNode);
            bestTimes.put(origin.getStopId(), 0.0);
        }

        int iterations = 0;
        log.info("Starting pathfinding from {} origins to {} destinations", origins.size(), destinations.size());

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            PathNode current = openSet.poll();

            // Create unique node identifier including trip context
            String nodeKey = current.stop().getStopId() + "_" +
                           (current.stopTime() != null ? current.stopTime().getTripId() : "start") + "_" +
                           current.transferCount();

            // Skip if already visited
            if (visitedNodes.contains(nodeKey)) {
                continue;
            }
            visitedNodes.add(nodeKey);

            // Skip if we found a better path to this stop already
            if (bestTimes.containsKey(current.stop().getStopId()) &&
                bestTimes.get(current.stop().getStopId()) < current.costFromStart()) {
                continue;
            }

            // Check if we reached destination
            if (destinationIds.contains(current.stop().getStopId())) {
                foundPaths.add(reconstructPath(current));
                log.info("Found path {} to destination {} with cost {}",
                        foundPaths.size(), current.stop().getStopId(), current.costFromStart());
                if (foundPaths.size() >= searchProperties.getMaxResults()) {
                    break;
                }
                continue;
            }

            // Check transfer limit
            if (current.transferCount() >= searchProperties.getMaxTransfers()) {
                continue;
            }

            // Calculate current time
            LocalDateTime currentNodeTime = (current.stopTime() == null)
                    ? queryBaseTime
                    : toLocalDateTime(current.stopTime().getArrivalTime(), date);

            // Add transfer buffer if we're transferring
            if (current.previous() != null && current.stopTime() != null &&
                current.previous().stopTime() != null &&
                !current.stopTime().getTripId().equals(current.previous().stopTime().getTripId())) {
                currentNodeTime = currentNodeTime.plusMinutes(TRANSFER_BUFFER_MINUTES);
            }

            // Check time limit
            if (currentNodeTime.isAfter(maxSearchTime)) {
                continue;
            }

            String searchTime = toGtfsTime(currentNodeTime);

            // Get departures from current stop (with limit)
            Set<StopTime> departures = cachedDataService.findDeparturesByStopAndTime(
                    current.stop().getStopId(), searchTime
            );

            int processedDepartures = 0;
            for (StopTime departure : departures) {
                if (processedDepartures++ >= MAX_DEPARTURES_PER_STOP) {
                    break;
                }

                Trip trip = cachedDataService.findTripById(departure.getTripId()).orElse(null);

                if (trip == null || !calendarService.canTravelForThisDate(trip.getServiceId(), date)) {
                    continue;
                }

                // Check if we're changing trips (transfer)
                boolean isTransfer = current.stopTime() != null &&
                                   !current.stopTime().getTripId().equals(trip.getTripId());

                // Get all next stops on this trip
                Set<StopTime> nextStops = cachedDataService.findNextStopsByTripAndSequence(
                        trip.getTripId(), departure.getStopSequence()
                );

                for (StopTime arrival : nextStops) {
                    LocalDateTime arrivalTime = toLocalDateTime(arrival.getArrivalTime(), date);

                    // Skip if arrival time exceeds search horizon
                    if (arrivalTime.isAfter(maxSearchTime)) {
                        continue;
                    }

                    double newCost = Duration.between(queryBaseTime, arrivalTime).toMinutes();

                    // Add transfer penalty if changing trips
                    if (isTransfer) {
                        newCost += searchProperties.getTransferPenaltyMinutes();
                    }

                    // Only add to queue if it's a better path
                    if (!bestTimes.containsKey(arrival.getStopId()) || newCost < bestTimes.get(arrival.getStopId())) {
                        bestTimes.put(arrival.getStopId(), newCost);
                        Stop nextStopObj = cachedDataService.findStopById(arrival.getStopId()).orElse(null);

                        if (nextStopObj == null) {
                            continue;
                        }

                        PathNode nextNode = PathNode.builder()
                                .stop(nextStopObj)
                                .stopTime(arrival)
                                .previous(current)
                                .costFromStart(newCost)
                                .transferCount(current.transferCount() + (isTransfer ? 1 : 0))
                                .build();
                        openSet.add(nextNode);
                    }
                }
            }

            // Log progress periodically
            if (iterations % 1000 == 0) {
                log.debug("Iterations: {}, OpenSet size: {}, Visited: {}, Paths found: {}",
                         iterations, openSet.size(), visitedNodes.size(), foundPaths.size());
            }
        }

        log.info("Pathfinding completed. Iterations: {}, Paths found: {}, Nodes visited: {}",
                iterations, foundPaths.size(), visitedNodes.size());

        if (iterations >= MAX_ITERATIONS) {
            log.warn("Reached maximum iteration limit. Results may be incomplete.");
        }

        return foundPaths;
    }

    private TravelPathBo reconstructPath(PathNode endNode) {
        LinkedList<GraphSegmentBo> segments = new LinkedList<>();
        PathNode current = endNode;

        while (current.previous() != null) {
            PathNode previous = current.previous();
            StopTime arrivalTimeInfo = current.stopTime();
            String tripId = arrivalTimeInfo.getTripId();

            StopTime departureTimeInfo = cachedDataService.findStopTimeByTripAndStop(tripId, previous.stop().getStopId())
                    .orElseThrow(() -> new IllegalStateException("Missing departure StopTime"));

            Trip trip = cachedDataService.findTripById(tripId).orElseThrow();

            segments.addFirst(GraphSegmentBo.builder()
                    .startStop(previous.stop())
                    .endStop(current.stop())
                    .startStopTime(departureTimeInfo)
                    .endStopTime(arrivalTimeInfo)
                    .trip(trip)
                    .route(cachedDataService.findRouteById(trip.getRouteId()).orElseThrow())
                    .isTransfer(true)
                    .cost(current.costFromStart() - previous.costFromStart())
                    .build());

            current = previous;
        }

        return new TravelPathBo(segments, endNode.costFromStart());
    }
}