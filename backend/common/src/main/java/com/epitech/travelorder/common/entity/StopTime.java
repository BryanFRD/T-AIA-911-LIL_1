package com.epitech.travelorder.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "stop_times")
@Getter
@Setter
public class StopTime {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "stop_id")
    private String stopId;

    @Column(name = "arrival_time")
    private String arrivalTime; // GTFS format special (ex: "25:30:00")

    @Column(name = "departure_time")
    private String departureTime; // GTFS format special (ex: "25:30:00")

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "pickup_type")
    private Integer pickupType;

    @Column(name = "drop_off_type")
    private Integer dropOffType;
}
