package com.epitech.travelorder.common.ml;

import lombok.Builder;

@Builder
public record MlFlowConfig (
        String trackingUri,
        String modelAlias,
        String modelCacheDir
) {
}
