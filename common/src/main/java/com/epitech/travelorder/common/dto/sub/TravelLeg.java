package com.epitech.travelorder.common.dto.sub;

import java.time.LocalDateTime;

public record TravelLeg(
        // Departure
        String originStopName,
        Double originLat,
        Double originLon,
        LocalDateTime departureTime,

        // Arrival
        Double destinationStopName,
        Double destinationLon,
        Double destinationLat,
        LocalDateTime arrivalTime,

        // Transport
        String routeShortName,   // "TGV, "TER", "BUS", etc.
        String routeLongName,    // "Paris - Lyon",
        String routeColor,
        String headSign
) {}
