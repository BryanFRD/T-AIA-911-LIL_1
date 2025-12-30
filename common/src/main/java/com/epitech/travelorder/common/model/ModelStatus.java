package com.epitech.travelorder.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Status information for a loaded model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatus {
    private ModelType modelType;
    private LoadStatus status;
    private String message;
    private Instant lastAttempt;
    private ModelMetadata metadata;

    public enum LoadStatus {
        LOADING,
        READY,
        FAILED,
        NOT_LOADED
    }

    public static ModelStatus loading(ModelType modelType) {
        return new ModelStatus(modelType, LoadStatus.LOADING, "Model is loading...", Instant.now(), null);
    }

    public static ModelStatus ready(ModelType modelType, ModelMetadata metadata) {
        return new ModelStatus(modelType, LoadStatus.READY, "Model loaded successfully", Instant.now(), metadata);
    }

    public static ModelStatus failed(ModelType modelType, String errorMessage) {
        return new ModelStatus(modelType, LoadStatus.FAILED, errorMessage, Instant.now(), null);
    }

    public static ModelStatus notLoaded(ModelType modelType) {
        return new ModelStatus(modelType, LoadStatus.NOT_LOADED, "Model not yet loaded", null, null);
    }
}
