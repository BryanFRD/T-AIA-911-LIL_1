package com.epitech.travelorder.common.model;

public record MlFlowConfig (
        String trackingUri,
        String modelAlias,
        String modelCacheDir
) {}
