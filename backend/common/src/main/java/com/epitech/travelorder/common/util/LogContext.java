package com.epitech.travelorder.common.util;

public record LogContext(String module, String context) {

    public static final String NLU = "NLU";
    public static final String ORCH = "ORCH";
    public static final String AUDIO = "AUDIO";
    public static final String PATHFINDER = "PATHFINDER";
    public static final String GTFS = "GTFS";
    public static final String COMMON = "COMMON";

    public static LogContext of(String module, String context) {
        return new LogContext(module, context);
    }

    public String format(String message) {
        return String.format("[%s:%s] %s", module, context, message);
    }

    public String format(String message, Object... args) {
        return String.format("[%s:%s] %s", module, context, String.format(message, args));
    }
}

