package com.epitech.travelorder.common.dto;

import com.epitech.travelorder.common.dto.sub.TravelProposal;

import java.util.List;

public record CustomerTravelSolution(
        RouteQuery customerQuery,
        List<TravelProposal> proposals
) {}
