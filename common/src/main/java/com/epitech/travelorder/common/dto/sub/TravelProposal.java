package com.epitech.travelorder.common.dto.sub;

import java.time.LocalDateTime;
import java.util.List;

public record TravelProposal(
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        TravelTime travelTime,
        List<TravelLeg> legs,
        Integer transferCount,
        Boolean multiDay
) {

    public TravelProposal(LocalDateTime departureTime, LocalDateTime arrivalTime, TravelTime travelTime, List<TravelLeg> legs) {
        this(departureTime, arrivalTime, travelTime, legs,
             legs != null ? legs.size() - 1 : 0,
             departureTime != null && arrivalTime != null &&
                     !departureTime.toLocalDate().equals(arrivalTime.toLocalDate()));
    }
}
