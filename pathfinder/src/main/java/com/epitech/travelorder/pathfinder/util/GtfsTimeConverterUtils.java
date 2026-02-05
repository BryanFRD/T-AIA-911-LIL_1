package com.epitech.travelorder.pathfinder.util;

import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class GtfsTimeConverterUtils {

    private static final DateTimeFormatter GTFS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String toGtfsTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(GTFS_TIME_FORMATTER);
    }

    public static LocalDateTime toLocalDateTime(String gtfsTime, LocalDate referenceDate) {
        if (gtfsTime == null || gtfsTime.isBlank()) {
            return null;
        }

        String[] parts = gtfsTime.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        if (hours >= 24) {
            LocalDate adjustedDate = referenceDate.plusDays(hours / 24);
            LocalTime time = LocalTime.of(hours % 24, minutes, seconds);
            return LocalDateTime.of(adjustedDate, time);
        }

        LocalTime time = LocalTime.of(hours, minutes, seconds);
        return LocalDateTime.of(referenceDate, time);
    }

    public static TravelTime calculateTravelTime(StopTime departure, StopTime arrival) {
        if (departure == null || arrival == null) {
            return new TravelTime(0, 0);
        }

        String departureTimeStr = departure.getDepartureTime();
        String arrivalTimeStr = arrival.getArrivalTime();

        long totalMinutes = calculateMinutesBetween(departureTimeStr, arrivalTimeStr);

        return new TravelTime((int) (totalMinutes / 60), (int) (totalMinutes % 60));
    }

    public static TravelTime calculateTravelTime(LocalDateTime departure, LocalDateTime arrival) {
        if (departure == null || arrival == null) {
            return new TravelTime(0, 0);
        }

        long totalMinutes = Duration.between(departure, arrival).toMinutes();
        if (totalMinutes < 0) {
            totalMinutes += 24 * 60;
        }

        return new TravelTime((int) (totalMinutes / 60), (int) (totalMinutes % 60));
    }

    public static long calculateMinutesBetween(String gtfsStartTime, String gtfsEndTime) {
        LocalTime start = parseGtfsTimeAsLocalTime(gtfsStartTime);
        LocalTime end = parseGtfsTimeAsLocalTime(gtfsEndTime);

        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }

        return minutes;
    }

    private static LocalTime parseGtfsTimeAsLocalTime(String gtfsTime) {
        String[] parts = gtfsTime.split(":");
        int hours = Integer.parseInt(parts[0]) % 24;
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return LocalTime.of(hours, minutes, seconds);
    }
}
