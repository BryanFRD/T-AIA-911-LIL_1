package com.epitech.travelorder.common.nlu.mlflow;

import com.epitech.travelorder.common.enumeration.LoadStatus;
import com.epitech.travelorder.common.enumeration.ModelType;

import java.time.Instant;

import static com.epitech.travelorder.common.enumeration.LoadStatus.FAILED;
import static com.epitech.travelorder.common.enumeration.LoadStatus.LOADING;
import static com.epitech.travelorder.common.enumeration.LoadStatus.NOT_LOADED;
import static com.epitech.travelorder.common.enumeration.LoadStatus.READY;

public record ModelStatus(ModelType modelType,
                          LoadStatus status,
                          String message,
                          Instant lastAttempt,
                          ModelMetadata metadata) {

    public static ModelStatus loading(ModelType modelType) {
        return new ModelStatus(modelType, LOADING, "Model is loading...", Instant.now(), null);
    }

    public static ModelStatus ready(ModelType modelType, ModelMetadata metadata) {
        return new ModelStatus(modelType, READY, "Model loaded successfully", Instant.now(), metadata);
    }

    public static ModelStatus failed(ModelType modelType, String errorMessage) {
        return new ModelStatus(modelType, FAILED, errorMessage, Instant.now(), null);
    }

    public static ModelStatus notLoaded(ModelType modelType) {
        return new ModelStatus(modelType, NOT_LOADED, "Model not yet loaded", null, null);
    }
}
