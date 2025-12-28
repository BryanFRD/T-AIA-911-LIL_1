package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.ml.MLflowConfig;
import com.epitech.travelorder.nlu.INLUService;
import com.epitech.travelorder.nlu.NLUPredictService;
import com.epitech.travelorder.pathfinder.PathfinderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hexagonal Architecture - Service Configuration
 * This class configures the core services used in the orchestrator module*/
@Configuration
public class ServicesConfig {

    @Value("${mlflow.tracking-uri:https://mlflow.bryan-ferrando.fr}")
    private String mlflowTrackingUri;

    @Value("${mlflow.model-alias:latest}")
    private String mlflowModelAlias;

    @Value("${mlflow.model-cache-dir:/tmp/djl-models}")
    private String mlflowModelCacheDir;

    @Bean
    public AudioService audioService() {
        return new AudioService();
    }

    @Bean
    public PathfinderService pathfinderService() {
        return new PathfinderService();
    }

    @Bean
    public INLUService nluService() {
        MLflowConfig mlflowConfig = new MLflowConfig();
        mlflowConfig.setTrackingUri(mlflowTrackingUri);
        mlflowConfig.setModelAlias(mlflowModelAlias);
        mlflowConfig.setModelCacheDir(mlflowModelCacheDir);
        return new NLUPredictService(mlflowConfig);
    }
}

