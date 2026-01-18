package com.epitech.travelorder.orchestrator.dto;

import java.util.List;

public record TravelRequest(String origin, String destination, List<String> viaPoints) {

    public TravelRequest(String origin, String destination) {
        this(origin, destination, List.of());
    }
}

