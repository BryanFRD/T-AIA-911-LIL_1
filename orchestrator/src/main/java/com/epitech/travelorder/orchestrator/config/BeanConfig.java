package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.nlu.mlflow.MlFlowConfig;
import com.epitech.travelorder.common.nlu.ModelLoader;
import com.epitech.travelorder.nlu.service.NluService;
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
public class BeanConfig {

    private String trackingUri;

    private String modelAlias;

    private String modelCacheDir;

    @Bean
    public MlFlowConfig mlFlowConfig() {
        log.info("Configuring MlFlow with trackingUri: {}, modelAlias: {}, modelCacheDir: {}",
                trackingUri, modelAlias, modelCacheDir);
        return new MlFlowConfig(trackingUri, modelAlias, modelCacheDir);
    }

    @Bean
    public ModelLoader modelLoader(MlFlowConfig mlFlowConfig) {
        return new ModelLoader(mlFlowConfig);
    }

    @Bean
    public AudioService audioService(ModelLoader modelLoader) {
        return new AudioService(modelLoader);
    }

    @Bean
    public NluService nluService(ModelLoader modelLoader) {
        return new NluService(modelLoader);
    }
}
