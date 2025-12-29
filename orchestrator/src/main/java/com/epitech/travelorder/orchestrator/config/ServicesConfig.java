package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.ml.MlFlowConfig;
import com.epitech.travelorder.nlu.INLUService;
import com.epitech.travelorder.nlu.NLUPredictService;
import com.epitech.travelorder.pathfinder.PathfinderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServicesConfig {

    @Value("${mlflow.tracking-uri}")
    private String mlflowTrackingUri;

    @Value("${mlflow.model-alias}")
    private String mlflowModelAlias;

    @Value("${mlflow.model-cache-dir}")
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
        MlFlowConfig mlflowConfig = MlFlowConfig.builder()
                .trackingUri(mlflowTrackingUri)
                .modelAlias(mlflowModelAlias)
                .modelCacheDir(mlflowModelCacheDir)
                .build();

        return new NLUPredictService(mlflowConfig);
    }
}

