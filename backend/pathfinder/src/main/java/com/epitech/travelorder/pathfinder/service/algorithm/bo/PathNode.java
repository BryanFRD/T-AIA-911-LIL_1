package com.epitech.travelorder.pathfinder.service.algorithm.bo;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import lombok.Builder;

import static java.lang.Double.compare;

@Builder
public record PathNode(
        StopTime stopTime,
        Stop stop,
        PathNode previous,
        double costFromStart,
        int transferCount
) implements Comparable<PathNode> {

    @Override
    public int compareTo(PathNode other) {
        return compare(this.costFromStart, other.costFromStart);
    }
}
