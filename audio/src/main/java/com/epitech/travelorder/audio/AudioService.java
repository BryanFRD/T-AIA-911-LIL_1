package com.epitech.travelorder.audio;

import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.infrastructure.Log;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.epitech.travelorder.common.domain.enumeration.ModelType.STT;
import static com.epitech.travelorder.common.domain.enumeration.ModelType.TTS;
import static com.epitech.travelorder.common.infrastructure.Log.AUDIO;

@Slf4j
@RequiredArgsConstructor
public class AudioService {

    private static final Log LOG_CONTEXT = Log.of(AUDIO, "AudioService");

    private final ModelLoader modelLoader;

    @PostConstruct
    public void init() {
        loadModel(STT);
        loadModel(TTS);
    }

    public String speechToText(byte[] audioData) {
        throw new UnsupportedOperationException("STT inference not yet implemented");
    }

    public byte[] textToSpeech(String text) {
        throw new UnsupportedOperationException("TTS inference not yet implemented");
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
}
