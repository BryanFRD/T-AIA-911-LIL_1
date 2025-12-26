package com.epitech.travelorder.gtfs.util;

public class TimeFormatter {

    private static final String TIME_SEPARATOR = ":";
    private static final int HOURS_IN_DAY = 24;

    private TimeFormatter() {
    }

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