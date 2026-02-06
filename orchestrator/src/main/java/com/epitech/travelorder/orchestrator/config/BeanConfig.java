package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.audio.util.VoskModelDownloader;
import com.epitech.travelorder.common.nlu.ModelLoader;
import com.epitech.travelorder.common.nlu.mlflow.MlFlowConfig;
import com.epitech.travelorder.nlu.service.NluService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

import java.io.IOException;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "bean-config")
@Getter
@Setter
public class BeanConfig {

    private String trackingUri;

    private String modelAlias;

    private String modelCacheDir;

    private String voskResourcePath;

    private String voskModelUrl;

    @Bean
    public Model voskModel() {
        try {
            VoskModelDownloader.ensureModelAvailable(voskResourcePath, voskModelUrl);
            log.info("Loading Vosk model from {}", voskResourcePath);
            return new Model(voskResourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download Vosk model from: " + voskModelUrl, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Vosk model from path: " + voskResourcePath, e);
        }
    }

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
    public AudioService audioService(Model voskModel) {
        return new AudioService(voskModel);
    }

    @Bean
    public NluService nluService(ModelLoader modelLoader) {
        return new NluService(modelLoader);
    }
}
