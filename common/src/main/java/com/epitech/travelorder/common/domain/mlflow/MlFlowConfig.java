package com.epitech.travelorder.common.domain.mlflow;

public record MlFlowConfig (
        String trackingUri,
        String modelAlias,
        String modelCacheDir
) {}
