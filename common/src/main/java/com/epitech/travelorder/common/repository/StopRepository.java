package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {

    @Query("SELECT s FROM Stop s WHERE LOWER(s.stopName) LIKE LOWER(CONCAT('%', :stopName, '%')) AND (s.parentStation IS NULL OR s.parentStation = '')")
    Set<Stop> findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(@Param("stopName") String stopName);

    @Query(value = """
        SELECT * FROM stops
        ORDER BY (POWER(stop_lat - :lat, 2) + POWER(stop_lon - :lon, 2)) ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<Stop> findNearestStation(@Param("lat") Double lat, @Param("lon") Double lon);
}

