package com.epitech.travelorder.common.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

    @Id
    @Column(name = "stop_id")
    private String stopId;

    @Column(name = "stop_name")
    private String stopName;

    @Column(name = "stop_lat")
    private Double stopLat;

    @Column(name = "stop_lon")
    private Double stopLon;

    @Column(name = "zone_id")
    private Integer zoneId;

    @Column(name = "location_type")
    private Integer locationType;

    @Column(name = "parent_station")
    private String parentStation;
}
