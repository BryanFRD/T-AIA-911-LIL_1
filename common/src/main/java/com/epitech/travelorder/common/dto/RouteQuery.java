package com.epitech.travelorder.common.dto;

import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public record RouteQuery(
        @With String origin,
        String destination,
        List<String> vias,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {

    public RouteQuery {
        vias = isEmpty(vias) ? emptyList() : copyOf(vias);
        departureTime = (departureTime == null || departureTime.isBefore(now()))
                ? now()
                : departureTime;
    }

    public boolean hasOrigin() {
        return isNotEmpty(origin);
    }

    public boolean hasDestination() { return isNotEmpty(destination);}

    public boolean hasVias() {
        return !isEmpty(vias);
    }
}

