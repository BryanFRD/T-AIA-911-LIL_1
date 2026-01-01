package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.model.MlFlowConfig;
import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.nlu.service.NluPredictService;
import com.epitech.travelorder.pathfinder.PathfinderService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "mlflow")
@Getter
@Setter
public class InternalBeanConfig {

    private String trackingUri;
    private String modelAlias;
    private String modelCacheDir;

    @Bean
    protected MlFlowConfig mlFlowConfig() {
        log.info("Configuring MlFlow with trackingUri: {}, modelAlias: {}, modelCacheDir: {}",
                trackingUri, modelAlias, modelCacheDir);
        return new MlFlowConfig(trackingUri, modelAlias, modelCacheDir);
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

