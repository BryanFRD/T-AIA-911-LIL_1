package com.epitech.travelorder.pathfinder.mapper;

import com.epitech.travelorder.common.dto.sub.TravelLeg;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import com.epitech.travelorder.pathfinder.util.bo.DirectTravelSolution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.List;

import static com.epitech.travelorder.pathfinder.util.TimeUtils.calculateTravelTime;
import static com.epitech.travelorder.pathfinder.util.TimeUtils.gtfsTimeToLocalDateTime;
import static org.mapstruct.factory.Mappers.getMapper;

@Mapper(uses = TravelLegMapper.class)
public interface TravelProposalMapper {

    TravelProposalMapper INSTANCE = getMapper(TravelProposalMapper.class);

    @Mapping(target = "departureTime", source = ".", qualifiedByName = "mapDepartureTime")
    @Mapping(target = "arrivalTime", source = ".", qualifiedByName = "mapArrivalTime")
    @Mapping(target = "travelTime", source = ".", qualifiedByName = "mapTravelTime")
    @Mapping(target = "legs", source = ".", qualifiedByName = "mapLegs")
    TravelProposal mapTravelProposal(DirectTravelSolution solution);

    @Named("mapDepartureTime")
    default LocalDateTime mapDepartureTime(DirectTravelSolution solution) {
        return gtfsTimeToLocalDateTime(
            solution.startStopTime().getDepartureTime(),
            solution.referenceDate()
        );
    }

    @Named("mapArrivalTime")
    default LocalDateTime mapArrivalTime(DirectTravelSolution solution) {
        return gtfsTimeToLocalDateTime(
            solution.endStopTime().getArrivalTime(),
            solution.referenceDate()
        );
    }

    @Named("mapTravelTime")
    default TravelTime mapTravelTime(DirectTravelSolution solution) {
        return calculateTravelTime(solution.startStopTime(), solution.endStopTime());
    }

    @Named("mapLegs")
    default List<TravelLeg> mapLegs(DirectTravelSolution solution) {
        return List.of(TravelLegMapper.INSTANCE.convert(
            solution.startStopTime(),
            solution.endStopTime(),
            solution.trip(),
            solution.route(),
            solution.origin(),
            solution.destination()
        ));
    }
}
