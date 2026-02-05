package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.StopTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface StopTimeRepository extends JpaRepository<StopTime, Long> {

    Optional<StopTime> findByTripIdAndStopId(final String tripId, final String stopId);

    Set<StopTime> findByStopIdAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(String stopId, String departureTime);

    Set<StopTime> findByTripIdAndStopSequenceGreaterThanOrderByStopSequenceAsc(String tripId, Integer stopSequence);
}
