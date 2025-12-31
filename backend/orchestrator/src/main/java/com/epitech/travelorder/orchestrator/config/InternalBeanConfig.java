package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.model.MlFlowConfig;
import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.nlu.service.NluPredictService;
import com.epitech.travelorder.pathfinder.PathfinderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class InternalBeanConfig {

    @Value("${mlflow.tracking-uri}")
    private String mlflowTrackingUri;

    @Value("${mlflow.model-alias}")
    private String mlflowModelAlias;

    @Value("${mlflow.model-cache-dir}")
    private String mlflowModelCacheDir;

    @Bean
    protected MlFlowConfig mlFlowConfig() {
        return new MlFlowConfig(mlflowTrackingUri, mlflowModelAlias, mlflowModelCacheDir);
    }

    @Bean
    protected ModelLoader modelLoader(MlFlowConfig mlFlowConfig) {
        return new ModelLoader(mlFlowConfig);
    }

    @Bean
    public AudioService audioService(ModelLoader modelLoader) {
        return new AudioService(modelLoader);
    }

    @Bean
    public PathfinderService pathfinderService() {
        return new PathfinderService();
    }

    @Bean
    public NluPredictService nluService(ModelLoader modelLoader) {
        return new NluPredictService(modelLoader);
    }
}

