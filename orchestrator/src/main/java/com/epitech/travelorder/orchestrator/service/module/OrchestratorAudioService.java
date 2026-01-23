package com.epitech.travelorder.orchestrator.service.module;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.orchestrator.config.exception.technical.AudioProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.epitech.travelorder.common.util.LogUtils.AUDIO;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorAudioService {

    private final AudioService audioService;

    public String transcribe(byte[] audioData) {
        try {
            return audioService.speechToText(audioData);
        } catch (Exception e) {
            throw new AudioProcessingException("Failed to transcribe audio", e);
        }
    }
}
