package com.epitech.travelorder.pathfinder.service.algorithm.bo;

import lombok.Builder;
import java.util.List;

@Builder
public record TravelPathBo(
        List<GraphSegmentBo> segments,
        double totalCost
) {}