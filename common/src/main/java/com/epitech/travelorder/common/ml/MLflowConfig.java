package com.epitech.travelorder.common.ml;

import lombok.Data;

/**
 * Configuration properties for MLflow integration
 */
@Data
public class MLflowConfig {

    /**
     * MLflow tracking server URI <a href="https://mlflow.bryan-ferrando.fr">MLFlow</a>
     */
    private String trackingUri = "https://mlflow.bryan-ferrando.fr";

    /**
     * MLflow registry URI (defaults to tracking URI if not specified)
     */
    private String registryUri;

    /**
     * Default experiment name for model tracking
     */
    private String experimentName = "travel-order";

    /**
     * Local cache directory for downloaded models
     */
    private String modelCacheDir = "/tmp/djl-models";

    /**
     * Model alias to load (latest, production, staging, champion, etc.)
     */
    private String modelAlias = "latest";

    public String getRegistryUri() {
        return registryUri != null ? registryUri : trackingUri;
    }
}
