package com.epitech.travelorder.common.nlu.mlflow;

public record MlFlowConfig (
        String trackingUri,
        String modelAlias,
        String modelCacheDir
) {}
