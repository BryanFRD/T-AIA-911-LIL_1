package com.epitech.travelorder.audio;

import com.epitech.travelorder.audio.util.AudioExtractorUtils;
import com.epitech.travelorder.audio.util.PcmUtils;
import com.epitech.travelorder.common.util.LogUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.util.Locale;

import static com.epitech.travelorder.common.util.LogUtils.AUDIO;
import static java.util.Locale.US;
import static java.util.Locale.getDefault;
import static java.util.Locale.setDefault;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioService {

    private static final LogUtils LOG_CONTEXT = LogUtils.of(AUDIO, "AudioService");
    private final Model voskModel;

    @PreDestroy
    public void cleanup() {
        if (voskModel != null) {
            voskModel.close();
        }
    }

    public String speechToText(byte[] audioData) {
        if (voskModel == null) {
            throw new IllegalStateException("Vosk model is not initialized");
        }

        try {
            byte[] pcmData = PcmUtils.convertAudioToPcm(audioData);
            String jsonResponse = runInference(pcmData);
            return AudioExtractorUtils.extractTextFromModelResponse(jsonResponse);

        } catch (IOException e) {
            log.error(LOG_CONTEXT.format("Failed to process audio transcription"), e);
            throw new RuntimeException("Failed to process audio transcription", e);
        }
    }

    private String runInference(byte[] pcmData) throws IOException {
        Locale originalLocale = getDefault();
        try {
            setDefault(US);

            try (Recognizer recognizer = new Recognizer(voskModel, 16000)) {
                recognizer.setMaxAlternatives(0);
                recognizer.setWords(true);

                if (recognizer.acceptWaveForm(pcmData, pcmData.length)) {
                    return recognizer.getResult();
                } else {
                    return recognizer.getFinalResult();
                }
            }
        } finally {
            // Restore original locale
            setDefault(originalLocale);
        }
    }

    public byte[] textToSpeech(String text) {
        throw new UnsupportedOperationException("TTS inference not yet implemented");
    }
}