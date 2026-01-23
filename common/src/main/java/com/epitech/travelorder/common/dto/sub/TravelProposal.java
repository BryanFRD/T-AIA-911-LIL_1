package com.epitech.travelorder.common.dto.sub;

import java.time.LocalDateTime;
import java.util.List;

public record TravelProposal(
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        TravelTime travelTime,
        List<TravelLeg> legs
) {}
