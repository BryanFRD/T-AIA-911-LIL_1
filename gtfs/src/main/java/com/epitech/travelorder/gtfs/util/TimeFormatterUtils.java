package com.epitech.travelorder.gtfs.util;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class TimeFormatterUtils {

    private static final String TIME_SEPARATOR = ":";

    private static final int HOURS_IN_DAY = 24;

    /**
     * In SNCF GTFS data, stop times can exceed 24 hours (e.g., "25:30:00").
     *
     * @param time the stop time string in the format "HH:MM:SS"
     * @return the formatted stop time string
     */
    public static String formatStopTime(String time) {
        if (time == null || time.isBlank()) {
            return time;
        }

        String[] parts = time.split(TIME_SEPARATOR);
        if (parts.length != 3) {
            return time;
        }

        try {
            int hours = Integer.parseInt(parts[0]);
            if (hours >= HOURS_IN_DAY) {
                hours = hours % HOURS_IN_DAY;
                return String.format("%02d:%s:%s", hours, parts[1], parts[2]);
            }
        } catch (NumberFormatException e) {
            return time;
        }

        return time;
    }
}