package com.epitech.travelorder.pathfinder;

import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.common.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteQueryEnricher {

    private static final LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "RouteQueryEnricher");

    private static final double MIN_LATITUDE = -90.0;

    private static final double MAX_LATITUDE = 90.0;

    private static final double MIN_LONGITUDE = -180.0;

    private static final double MAX_LONGITUDE = 180.0;

    private final StopRepository stopRepository;

    public RouteQuery enrich(RouteQuery routeQuery, Double lat, Double lon) {
        if (routeQuery.hasOrigin()) {
            return routeQuery;
        }

        if (!isValidCoordinate(lat, lon)) {
            log.warn(LOG_CTX.format("Cannot enrich origin: invalid or missing coordinates (lat=%s, lon=%s)", lat, lon));
            return routeQuery;
        }

        return stopRepository.findNearestStation(lat, lon)
                .map(stop -> {
                    log.info(LOG_CTX.format("Enriched route query origin with nearest station: %s (ID: %s) for coordinates: lat=%s, lon=%s", stop.getStopName(), stop.getStopId(), lat, lon));
                    return routeQuery.withOrigin(stop.getStopName());
                })
                .orElseGet(() -> {
                    log.warn(LOG_CTX.format("No station found near coordinates: lat=%s, lon=%s", lat, lon));
                    return routeQuery;
                });
    }

    private boolean isValidCoordinate(Double lat, Double lon) {
        return lat != null && lon != null
                && lat >= MIN_LATITUDE && lat <= MAX_LATITUDE
                && lon >= MIN_LONGITUDE && lon <= MAX_LONGITUDE;
    }
}