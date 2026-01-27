package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findAllByStopNameContainingIgnoreCase(String stopName);

    // (x - a)² + (y - b)² is minimized when the point (x, y) is closest to (a, b)
    @Query(value = """
        SELECT * FROM stops 
        ORDER BY (POWER(stop_lat - :lat, 2) + POWER(stop_lon - :lon, 2)) ASC 
        LIMIT 1
    """, nativeQuery = true)
    Optional<Stop> findNearestStation(@Param("lat") Double lat, @Param("lon") Double lon);
}

