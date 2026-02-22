package com.epitech.travelorder.pathfinder.mapper;

import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.GraphSegmentBo;
import com.epitech.travelorder.pathfinder.service.algorithm.bo.DirectTravelSolution;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.calculateTravelTime;
import static com.epitech.travelorder.pathfinder.util.GtfsTimeConverterUtils.toLocalDateTime;
import static org.mapstruct.factory.Mappers.getMapper;

@Mapper
public interface TimeMapper {

    TimeMapper INSTANCE = getMapper(TimeMapper.class);

    @Named("mapGtfsString")
    default LocalDateTime mapGtfsString(String gtfsTime, @Context LocalDate date) {
        return toLocalDateTime(gtfsTime, date);
    }

    @Named("mapDepartureTime")
    default LocalDateTime mapDepartureTime(GraphSegmentBo segment, @Context LocalDate date) {
        return toLocalDateTime(segment.startStopTime().getDepartureTime(), date);
    }

    @Named("mapArrivalTime")
    default LocalDateTime mapArrivalTime(GraphSegmentBo segment, @Context LocalDate date) {
        return toLocalDateTime(segment.endStopTime().getArrivalTime(), date);
    }

    @Named("mapDepartureTimeDirect")
    default LocalDateTime mapDepartureTime(DirectTravelSolution solution) {
        return toLocalDateTime(solution.startStopTime().getDepartureTime(), solution.referenceDate());
    }

    @Named("mapArrivalTimeDirect")
    default LocalDateTime mapArrivalTime(DirectTravelSolution solution) {
        LocalDateTime departure = toLocalDateTime(solution.startStopTime().getDepartureTime(), solution.referenceDate());
        LocalDateTime arrival = toLocalDateTime(solution.endStopTime().getArrivalTime(), solution.referenceDate());

        // If arrival is before departure, it means we cross midnight (overnight train)
        if (arrival != null && departure != null && arrival.isBefore(departure)) {
            arrival = toLocalDateTime(solution.endStopTime().getArrivalTime(), solution.referenceDate().plusDays(1));
        }

        return arrival;
    }

    @Named("mapTravelTime")
    default TravelTime mapTravelTime(DirectTravelSolution solution) {
        return calculateTravelTime(solution.startStopTime(), solution.endStopTime());
    }

    @Named("mapGtfsStringToString")
    default String mapGtfsStringToString(String gtfsTime, @Context LocalDate date) {
        LocalDateTime dateTime = toLocalDateTime(gtfsTime, date);
        return dateTime != null ? dateTime.toString() : null;
    }

    @Named("mapDepartureTimeToString")
    default String mapDepartureTimeToString(DirectTravelSolution solution, @Context LocalDate date) {
        LocalDateTime departure = toLocalDateTime(solution.startStopTime().getDepartureTime(), date);
        return departure != null ? departure.toString() : null;
    }

    @Named("mapArrivalTimeToString")
    default String mapArrivalTimeToString(DirectTravelSolution solution, @Context LocalDate date) {
        LocalDateTime departure = toLocalDateTime(solution.startStopTime().getDepartureTime(), date);
        LocalDateTime arrival = toLocalDateTime(solution.endStopTime().getArrivalTime(), date);

        // If arrival is before departure, we crossed midnight
        if (arrival != null && departure != null && arrival.isBefore(departure)) {
            arrival = toLocalDateTime(solution.endStopTime().getArrivalTime(), date.plusDays(1));
        }

        return arrival != null ? arrival.toString() : null;
    }

    @Named("mapDepartureTimeToStringSegment")
    default String mapDepartureTimeToStringSegment(GraphSegmentBo segment, @Context LocalDate date) {
        LocalDateTime departure = toLocalDateTime(segment.startStopTime().getDepartureTime(), date);
        return departure != null ? departure.toString() : null;
    }

    @Named("mapArrivalTimeToStringSegment")
    default String mapArrivalTimeToStringSegment(GraphSegmentBo segment, @Context LocalDate date) {
        LocalDateTime departure = toLocalDateTime(segment.startStopTime().getDepartureTime(), date);
        LocalDateTime arrival = toLocalDateTime(segment.endStopTime().getArrivalTime(), date);

        // If arrival is before departure, we crossed midnight
        if (arrival != null && departure != null && arrival.isBefore(departure)) {
            arrival = toLocalDateTime(segment.endStopTime().getArrivalTime(), date.plusDays(1));
        }

        return arrival != null ? arrival.toString() : null;
    }

    @Named("mapTravelTimeFromStopTimes")
    default TravelTime mapTravelTimeFromStopTimes(DirectTravelSolution solution) {
        return calculateTravelTime(solution.startStopTime(), solution.endStopTime());
    }

    @Named("mapTravelTimeFromSegment")
    default TravelTime mapTravelTimeFromSegment(GraphSegmentBo segment) {
        return calculateTravelTime(segment.startStopTime(), segment.endStopTime());
    }
}