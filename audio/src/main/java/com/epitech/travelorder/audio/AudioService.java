package com.epitech.travelorder.audio;

import com.epitech.travelorder.common.model.MlFlowConfig;
import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.common.model.ModelType;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

/**
 * Audio service implementation for speech-to-text and text-to-speech
 */
@Slf4j
public class AudioService implements IAudioService {

    private final ModelLoader modelLoader;

    public AudioService(MlFlowConfig mlflowConfig) {
        this.modelLoader = new ModelLoader(mlflowConfig);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing audio models...");
        loadSTTModel();
        loadTTSModel();
        log.info("Audio models initialization completed");
    }

    private void loadSTTModel() {
        try {
            log.info("Loading STT model from MLflow");
            modelLoader.ensureArtifactsDownloaded(ModelType.STT);
            log.info("STT model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load STT model: {}", e.getMessage(), e);
        }
    }

    private void loadTTSModel() {
        try {
            log.info("Loading TTS model from MLflow");
            modelLoader.ensureArtifactsDownloaded(ModelType.TTS);
            log.info("TTS model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load TTS model: {}", e.getMessage(), e);
        }
    }

    @Override
    public String speechToText(byte[] audioData) {
        throw new UnsupportedOperationException("STT inference not yet implemented");
    }

    @Override
    public byte[] textToSpeech(String text) {
        throw new UnsupportedOperationException("TTS inference not yet implemented");
    }

    public ModelLoader getModelLoader() {
        return modelLoader;
    }
}
