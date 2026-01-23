package com.epitech.travelorder.common.dto;

import lombok.With;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.List.copyOf;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public record RouteQuery(
        @With String origin,
        String destination,
        List<String> vias,
        LocalDateTime departureTime
) {

    public RouteQuery {
        vias = isEmpty(vias) ? emptyList() : copyOf(vias);
    }

    public boolean hasOrigin() {
        return isNotEmpty(origin);
    }

    public boolean hasVias() {
        return !isEmpty(vias);
    }

    public boolean hasDepartureTime() {
        return departureTime != null;
    }
}

