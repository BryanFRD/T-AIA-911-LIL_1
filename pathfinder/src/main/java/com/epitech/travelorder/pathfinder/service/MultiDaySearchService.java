package com.epitech.travelorder.pathfinder.service;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.pathfinder.config.PathfinderSearchProperties;
import com.epitech.travelorder.pathfinder.service.algorithm.DijkstraPathfinderService;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.TravelPathBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toGtfsTime;
import static java.util.Comparator.comparingDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDaySearchService {

    private final DijkstraPathfinderService dijkstraService;

    private final PathfinderSearchProperties searchProperties;

    public List<TravelPathBo> findPathsWithHorizon(
            Set<Stop> originStops,
            Set<Stop> destinationStops,
            LocalDateTime requestedDepartureTime
    ) {
        List<TravelPathBo> allResults = new ArrayList<>();
        int horizonDays = searchProperties.getSearchHorizonDays();

        for (int dayOffset = 0; dayOffset < horizonDays; dayOffset++) {
            LocalDate searchDate = requestedDepartureTime.toLocalDate().plusDays(dayOffset);
            String gtfsTime = dayOffset == 0
                    ? toGtfsTime(requestedDepartureTime)
                    : "00:00:00";

            log.debug("Searching on day offset {} ({})", dayOffset, searchDate);

            List<TravelPathBo> dayResults = dijkstraService.findPaths(
                    originStops,
                    destinationStops,
                    gtfsTime,
                    searchDate
            );

            allResults.addAll(dayResults);

            if (allResults.size() >= searchProperties.getMaxResults()) {
                break;
            }
        }

        return allResults.stream()
                .sorted(comparingDouble(TravelPathBo::totalCost))
                .limit(searchProperties.getMaxResults())
                .toList();
    }
}
