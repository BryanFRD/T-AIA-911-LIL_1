package com.epitech.travelorder.common.domain.nlu;

import java.util.List;

public record NluStatus(
    String status,
    List<NluModelInfo> models
) {}

