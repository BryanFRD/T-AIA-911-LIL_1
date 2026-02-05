package com.epitech.travelorder.pathfinder.mapper;

import com.epitech.travelorder.common.dto.sub.TravelLeg;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.DirectTravelSolution;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.GraphSegmentBo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

import static org.mapstruct.factory.Mappers.getMapper;

@Mapper(uses = TimeMapper.class)
public interface TravelLegMapper {

    TravelLegMapper INSTANCE = getMapper(TravelLegMapper.class);

    @Mapping(target = "originStopName", source = "source.origin.stopName")
    @Mapping(target = "originLat", source = "source.origin.stopLat")
    @Mapping(target = "originLon", source = "source.origin.stopLon")
    @Mapping(target = "departureTime", source = "source.startStopTime.departureTime", qualifiedByName = "mapGtfsStringToString")
    @Mapping(target = "destinationStopName", source = "source.destination.stopName")
    @Mapping(target = "destinationLat", source = "source.destination.stopLat")
    @Mapping(target = "destinationLon", source = "source.destination.stopLon")
    @Mapping(target = "arrivalTime", source = "source.endStopTime.arrivalTime", qualifiedByName = "mapGtfsStringToString")
    @Mapping(target = "routeShortName", source = "source.route.routeShortName")
    @Mapping(target = "routeLongName", source = "source.route.routeLongName")
    @Mapping(target = "routeColor", source = "source.route.routeTextColor")
    @Mapping(target = "headSign", source = "source.trip.tripHeadsign")
    @Mapping(target = "travelTime", source = ".", qualifiedByName = "mapTravelTimeFromStopTimes")
    TravelLeg convert(DirectTravelSolution source, @Context LocalDate date);

    @Mapping(target = "originStopName", source = "source.startStop.stopName")
    @Mapping(target = "originLat", source = "source.startStop.stopLat")
    @Mapping(target = "originLon", source = "source.startStop.stopLon")
    @Mapping(target = "departureTime", source = "source.startStopTime.departureTime", qualifiedByName = "mapGtfsStringToString")
    @Mapping(target = "destinationStopName", source = "source.endStop.stopName")
    @Mapping(target = "destinationLat", source = "source.endStop.stopLat")
    @Mapping(target = "destinationLon", source = "source.endStop.stopLon")
    @Mapping(target = "arrivalTime", source = "source.endStopTime.arrivalTime", qualifiedByName = "mapGtfsStringToString")
    @Mapping(target = "routeShortName", source = "source.route.routeShortName")
    @Mapping(target = "routeLongName", source = "source.route.routeLongName")
    @Mapping(target = "routeColor", source = "source.route.routeTextColor")
    @Mapping(target = "headSign", source = "source.trip.tripHeadsign")
    @Mapping(target = "travelTime", source = ".", qualifiedByName = "mapTravelTimeFromSegment")
    TravelLeg convert(GraphSegmentBo source, @Context LocalDate date);
}