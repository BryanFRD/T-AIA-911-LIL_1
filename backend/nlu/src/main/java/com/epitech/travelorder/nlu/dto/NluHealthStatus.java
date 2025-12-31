package com.epitech.travelorder.nlu.dto;

import java.util.List;

public record NluHealthStatus(
    String status,
    List<ModelInfo> models
) {}

