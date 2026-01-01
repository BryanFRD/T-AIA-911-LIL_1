package com.epitech.travelorder.nlu.dto;

import java.util.List;

public record NluStatus(
    String status,
    List<ModelInfo> models
) {}

