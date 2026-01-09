package com.epitech.travelorder.orchestrator.infrastructure.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.domain.mlflow.MlFlowConfig;
import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.application.usecase.FindTravelPathUseCaseImpl;
import com.epitech.travelorder.orchestrator.application.usecase.TranscribeAudioUseCaseImpl;
import com.epitech.travelorder.orchestrator.domain.port.in.FindTravelPathUseCase;
import com.epitech.travelorder.orchestrator.domain.port.in.TranscribeAudioUseCase;
import com.epitech.travelorder.orchestrator.domain.port.out.AudioPort;
import com.epitech.travelorder.orchestrator.domain.port.out.NluPort;
import com.epitech.travelorder.orchestrator.domain.port.out.PathfinderPort;
import com.epitech.travelorder.orchestrator.domain.service.TravelRequestExtractor;
import com.epitech.travelorder.orchestrator.infrastructure.adapter.out.audio.AudioServiceAdapter;
import com.epitech.travelorder.orchestrator.infrastructure.adapter.out.nlu.NluServiceAdapter;
import com.epitech.travelorder.orchestrator.infrastructure.adapter.out.pathfinder.PathfinderServiceAdapter;
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
        return new PathfinderService(new com.epitech.travelorder.pathfinder.infrastructure.adapter.persistence.StopRepositoryStubAdapter());
    }

    @Bean
    public NluService nluService(ModelLoader modelLoader) {
        return new NluService(modelLoader);
    }

    @Bean
    public AudioPort audioPort(AudioService audioService) {
        return new AudioServiceAdapter(audioService);
    }

    @Bean
    public NluPort nluPort(NluService nluService) {
        return new NluServiceAdapter(nluService);
    }

    @Bean
    public PathfinderPort pathfinderPort(PathfinderService pathfinderService) {
        return new PathfinderServiceAdapter(pathfinderService);
    }

    @Bean
    public TravelRequestExtractor travelRequestExtractor() {
        return new TravelRequestExtractor();
    }

    @Bean
    public TranscribeAudioUseCase transcribeAudioUseCase(AudioPort audioPort) {
        return new TranscribeAudioUseCaseImpl(audioPort);
    }

    @Bean
    public FindTravelPathUseCase findTravelPathUseCase(
            NluPort nluPort,
            PathfinderPort pathfinderPort,
            TravelRequestExtractor travelRequestExtractor) {
        return new FindTravelPathUseCaseImpl(nluPort, pathfinderPort, travelRequestExtractor);
    }
}

