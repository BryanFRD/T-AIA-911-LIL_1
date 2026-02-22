package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.StopTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface StopTimeRepository extends JpaRepository<StopTime, Long> {

    Optional<StopTime> findByTripIdAndStopId(final String tripId, final String stopId);

    @Query(value = """
        SELECT * FROM stop_times
        WHERE stop_id = :stopId
        AND departure_time >= :departureTime
        ORDER BY departure_time ASC
        LIMIT 100
    """, nativeQuery = true)
    Set<StopTime> findByStopIdAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
            @Param("stopId") String stopId,
            @Param("departureTime") String departureTime
    );

    @Query(value = """
        SELECT * FROM stop_times
        WHERE trip_id = :tripId
        AND stop_sequence > :stopSequence
        ORDER BY stop_sequence ASC
        LIMIT 50
    """, nativeQuery = true)
    Set<StopTime> findByTripIdAndStopSequenceGreaterThanOrderByStopSequenceAsc(
            @Param("tripId") String tripId,
            @Param("stopSequence") Integer stopSequence
    );
}
