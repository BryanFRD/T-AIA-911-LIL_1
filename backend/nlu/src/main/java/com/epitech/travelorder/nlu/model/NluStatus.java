package com.epitech.travelorder.nlu.model;

import java.util.List;

public record NluStatus(
    String status,
    List<NluModelInfo> models
) {}

