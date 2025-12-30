package com.epitech.travelorder.common.model;

import lombok.Builder;

@Builder
public record MlFlowConfig (
        String trackingUri,
        String modelAlias,
        String modelCacheDir
) {
}
