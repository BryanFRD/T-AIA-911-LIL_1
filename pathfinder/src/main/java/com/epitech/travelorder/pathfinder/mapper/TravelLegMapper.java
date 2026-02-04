package com.epitech.travelorder.pathfinder.mapper;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.common.dto.sub.TravelLeg;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.factory.Mappers.getMapper;

@Mapper
public interface TravelLegMapper {

    TravelLegMapper INSTANCE = getMapper(TravelLegMapper.class);

    @Mapping(target = "originStopName", source = "origin.stopName")
    @Mapping(target = "originLat", source = "origin.stopLat")
    @Mapping(target = "originLon", source = "origin.stopLon")
    @Mapping(target = "departureTime", source = "startStopTime.departureTime")
    @Mapping(target = "destinationStopName", source = "destination.stopName")
    @Mapping(target = "destinationLat", source = "destination.stopLat")
    @Mapping(target = "destinationLon", source = "destination.stopLon")
    @Mapping(target = "arrivalTime", source = "endStopTime.arrivalTime")
    @Mapping(target = "routeShortName", source = "route.routeShortName")
    @Mapping(target = "routeLongName", source = "route.routeLongName")
    @Mapping(target = "routeColor", source = "route.routeTextColor")
    @Mapping(target = "headSign", source = "trip.tripHeadsign")
    TravelLeg convert(
            StopTime startStopTime,
            StopTime endStopTime,
            Trip trip,
            Route route,
            Stop origin,
            Stop destination
    );
}
