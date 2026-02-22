package com.epitech.travelorder.common.dto;

import com.epitech.travelorder.common.dto.sub.TravelProposal;

import java.util.List;

import static java.util.Collections.emptyList;

public record CustomerTravelSolution(
        RouteQuery customerQuery,
        List<TravelProposal> proposals
) {

    public static CustomerTravelSolution empty(RouteQuery query) {
        return new CustomerTravelSolution(query, emptyList());
    }
}
