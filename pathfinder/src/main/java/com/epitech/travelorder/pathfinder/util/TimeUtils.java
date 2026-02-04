package com.epitech.travelorder.pathfinder.util;

import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.dto.sub.TravelTime;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

import static java.time.format.DateTimeFormatter.ofPattern;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class TimeUtils {

    public static String convertLocalDateTimeToGtfsDate(final LocalDateTime localDateTime) {
        return localDateTime.format(ofPattern("HH:mm:ss"));
    }

    public static LocalDateTime gtfsTimeToLocalDateTime(final String gtfsTime, final LocalDate referenceDate) {
        if (gtfsTime == null || gtfsTime.isBlank()) {
            return null;
        }
        LocalTime time = LocalTime.parse(gtfsTime);
        return LocalDateTime.of(referenceDate, time);
    }

    public static TravelTime calculateTravelTime(final StopTime startStopTime, final StopTime endStopTime) {
        String departureTimeStr = startStopTime.getDepartureTime();
        String arrivalTimeStr = endStopTime.getArrivalTime();

        LocalTime departureTime = LocalTime.parse(departureTimeStr);
        LocalTime arrivalTime = LocalTime.parse(arrivalTimeStr);

        long totalMinutes = Duration.between(departureTime, arrivalTime).toMinutes();
        if (totalMinutes < 0) {
            totalMinutes += 24 * 60;
        }

        return new TravelTime((int) (totalMinutes / 60), (int) (totalMinutes % 60));
    }
}
