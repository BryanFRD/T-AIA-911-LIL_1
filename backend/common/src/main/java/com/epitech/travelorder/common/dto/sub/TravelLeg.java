package com.epitech.travelorder.common.dto.sub;

public record TravelLeg(
        // Departure
        String originStopName,
        Double originLat,
        Double originLon,
        String departureTime,

        // Arrival
        String destinationStopName,
        Double destinationLat,
        Double destinationLon,
        String arrivalTime,

        // Transport
        String routeShortName,   // "TGV, "TER", "BUS", etc.
        String routeLongName,    // "Paris - Lyon",
        String routeColor,
        String headSign,

        // Duration
        TravelTime travelTime
) {

}
