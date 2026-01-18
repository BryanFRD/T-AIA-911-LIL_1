package com.epitech.travelorder.pathfinder.service;

import com.epitech.travelorder.common.domain.sncf.Stop;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.pathfinder.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathfindingService {

    private static final LogUtils LOG_UTILS_CTX = LogUtils.of(LogUtils.PATHFINDER, "RouteFinder");

    private final StopRepository stopRepository;

    public String findRoute(String origin, String destination) {
        log.info(LOG_UTILS_CTX.format("Finding route from %s to %s", origin, destination));

        List<Stop> origins = stopRepository.findAllByStopNameContainingIgnoreCase(origin);
        List<Stop> destinations = stopRepository.findAllByStopNameContainingIgnoreCase(destination);

        if (origins.isEmpty()) {
            log.warn(LOG_UTILS_CTX.format("No stops found matching origin: %s", origin));
            return "No route found: origin not found.";
        }

        if (destinations.isEmpty()) {
            log.warn(LOG_UTILS_CTX.format("No stops found matching destination: %s", destination));
            return "No route found: destination not found.";
        }

        // for now we just take the first matching stops
        Stop originStop = origins.getFirst();
        Stop destinationStop = destinations.getFirst();

        String routeInfo = String.format("Route found from %s (ID: %s) to %s (ID: %s)",
                originStop.getStopName(), originStop.getStopId(),
                destinationStop.getStopName(), destinationStop.getStopId());

        log.info(LOG_UTILS_CTX.format(routeInfo));
        return routeInfo;
    }

    public String findRouteWithVia(String origin, String destination, List<String> viaPoints) {
        log.info(LOG_UTILS_CTX.format("Finding route from {} to {} via {}",
                origin, destination, String.join(", ", viaPoints)));

        StringBuilder path = new StringBuilder();
        String currentOrigin = origin;

        for (String via : viaPoints) {
            String segment = findRoute(currentOrigin, via);
            path.append(segment).append("\n");
            currentOrigin = via;
        }

        String finalSegment = findRoute(currentOrigin, destination);
        path.append(finalSegment);

        return path.toString();
    }
}
