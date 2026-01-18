package com.epitech.travelorder.common.util;

public record LogUtils(String serviceName, String contextName) {

    public static final String ORCHESTRATOR = "ORCH";
    public static final String NLU = "NLU";
    public static final String PATHFINDER = "PATH";
    public static final String GTFS = "GTFS";
    public static final String AUDIO = "AUDIO";
    public static final String COMMON = "COMMON";

    public static LogUtils of(String serviceName, String contextName) {
        return new LogUtils(serviceName, contextName);
    }

    public String format(String message, Object... args) {
        String formattedMessage = args.length > 0 ? String.format(message, args) : message;
        return String.format("[%s:%s] %s", serviceName, contextName, formattedMessage);
    }
}

