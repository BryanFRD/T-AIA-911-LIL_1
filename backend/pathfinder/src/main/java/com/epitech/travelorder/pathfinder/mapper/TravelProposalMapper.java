package com.epitech.travelorder.pathfinder.mapper;

import com.epitech.travelorder.common.dto.sub.TravelLeg;
import com.epitech.travelorder.common.dto.sub.TravelProposal;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.DirectTravelSolution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

import static org.mapstruct.factory.Mappers.getMapper;

@Mapper(uses = {TravelLegMapper.class, TimeMapper.class})
public interface TravelProposalMapper {

    TravelProposalMapper INSTANCE = getMapper(TravelProposalMapper.class);

    @Mapping(target = "departureTime", source = ".", qualifiedByName = "mapDepartureTimeDirect")
    @Mapping(target = "arrivalTime", source = ".", qualifiedByName = "mapArrivalTimeDirect")
    @Mapping(target = "travelTime", source = ".", qualifiedByName = "mapTravelTime")
    @Mapping(target = "legs", source = ".", qualifiedByName = "mapLegs")
    @Mapping(target = "transferCount", constant = "0")
    @Mapping(target = "multiDay", source = ".", qualifiedByName = "mapMultiDay")
    TravelProposal convert(DirectTravelSolution solution);

    @Named("mapLegs")
    default List<TravelLeg> mapLegs(DirectTravelSolution solution) {
        return List.of(TravelLegMapper.INSTANCE.convert(solution, solution.referenceDate()));
    }

    @Named("mapMultiDay")
    default Boolean mapMultiDay(DirectTravelSolution solution) {
        return TimeMapper.INSTANCE.mapDepartureTime(solution)
                .toLocalDate()
                .isBefore(TimeMapper.INSTANCE.mapArrivalTime(solution).toLocalDate());
    }
}