package com.epitech.travelorder.audio;

import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.common.model.ModelType;
import com.epitech.travelorder.common.util.LogContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

import static com.epitech.travelorder.common.util.LogContext.AUDIO;

@Slf4j
@RequiredArgsConstructor
public class AudioService {

    private static final LogContext LOG_CONTEXT = LogContext.of(AUDIO, "AudioService");

    @Getter
    private final ModelLoader modelLoader;

    @PostConstruct
    public void init() {
        log.info(LOG_CONTEXT.format("Load audio models at startup"));
        loadModel(ModelType.STT);
        loadModel(ModelType.TTS);
        log.info(LOG_CONTEXT.format("Audio models loaded with success"));
    }

    private void loadModel(ModelType modelType) {
        try {
            log.info(LOG_CONTEXT.format("Loading model: %s", modelType));
            modelLoader.ensureArtifactsDownloaded(modelType);
            log.info(LOG_CONTEXT.format("Model %s loaded successfully", modelType));
        } catch (Exception e) {
            log.error(LOG_CONTEXT.format("Failed to load model: %s", modelType), e);
        }
    }

    public String speechToText(byte[] audioData) {
        throw new UnsupportedOperationException("STT inference not yet implemented");
    }

    public byte[] textToSpeech(String text) {
        throw new UnsupportedOperationException("TTS inference not yet implemented");
    }
}
