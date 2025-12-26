package com.epitech.travelorder.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id")
    private String routeId; // insert for GTFS

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "trip_headsign")
    private String tripHeadsign;

    @Column(name = "direction_id")
    private Integer directionId;

    @Column(name = "block_id")
    private String blockId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    private Route route;
}
