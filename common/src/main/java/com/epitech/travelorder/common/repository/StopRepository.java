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

    @Query("""
        SELECT s FROM Stop s
        WHERE LOWER(s.stopName) LIKE LOWER(CONCAT('%', :stopName, '%'))
        AND s.parentStation IS NOT NULL
        AND s.parentStation != ''
        AND s.locationType = 0
        AND (s.stopId LIKE '%Train TER%'
             OR s.stopId LIKE '%TGV INOUI%'
             OR s.stopId LIKE '%OUIGO%'
             OR s.stopId LIKE '%INTERCITES%')
        ORDER BY LENGTH(s.stopName) ASC
        """)
    Set<Stop> findAllByStopNameContainingIgnoreCaseAndParentStationEmpty(@Param("stopName") String stopName);

    @Query(value = """
        SELECT * FROM stops
        WHERE parent_station IS NOT NULL
        AND parent_station != ''
        AND location_type = 0
        AND (stop_id LIKE '%Train TER%'
             OR stop_id LIKE '%TGV INOUI%'
             OR stop_id LIKE '%OUIGO%'
             OR stop_id LIKE '%INTERCITES%')
        AND stop_lat BETWEEN :lat - 1 AND :lat + 1
        AND stop_lon BETWEEN :lon - 1 AND :lon + 1
        ORDER BY (POWER(stop_lat - :lat, 2) + POWER(stop_lon - :lon, 2)) ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<Stop> findNearestStation(@Param("lat") Double lat, @Param("lon") Double lon);
}

