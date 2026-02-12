package com.epitech.travelorder.pathfinder.service;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.common.repository.RouteRepository;
import com.epitech.travelorder.common.repository.StopRepository;
import com.epitech.travelorder.common.repository.StopTimeRepository;
import com.epitech.travelorder.common.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaffeineCacheService {

    private final StopTimeRepository stopTimeRepository;

    private final TripRepository tripRepository;

    private final RouteRepository routeRepository;

    private final StopRepository stopRepository;

    @Cacheable(value = "stopTimes", key = "#stopId + '_' + #searchTime")
    public Set<StopTime> findDeparturesByStopAndTime(String stopId, String searchTime) {
        return stopTimeRepository.findByStopIdAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
                stopId, searchTime
        );
    }

    @Cacheable(value = "stopTimes", key = "#tripId + '_seq_' + #stopSequence")
    public Set<StopTime> findNextStopsByTripAndSequence(String tripId, int stopSequence) {
        return stopTimeRepository.findByTripIdAndStopSequenceGreaterThanOrderByStopSequenceAsc(
                tripId, stopSequence
        );
    }

    @Cacheable(value = "stopTimes", key = "#tripId + '_' + #stopId")
    public Optional<StopTime> findStopTimeByTripAndStop(String tripId, String stopId) {
        return stopTimeRepository.findByTripIdAndStopId(tripId, stopId);
    }

    @Cacheable(value = "trips", key = "#tripId")
    public Optional<Trip> findTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    @Cacheable(value = "routes", key = "#routeId")
    public Optional<Route> findRouteById(String routeId) {
        return routeRepository.findById(routeId);
    }

    @Cacheable(value = "stops", key = "#stopId")
    public Optional<Stop> findStopById(String stopId) {
        return stopRepository.findById(stopId);
    }

    @Cacheable(value = "stops", key = "'name_' + #stopName")
    public Set<Stop> findStopsByName(String stopName) {
        return stopRepository.findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(stopName);
    }
}
