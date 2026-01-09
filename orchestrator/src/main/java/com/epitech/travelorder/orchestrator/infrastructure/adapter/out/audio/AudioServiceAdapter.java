package com.epitech.travelorder.orchestrator.infrastructure.adapter.out.audio;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.orchestrator.domain.port.out.AudioPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AudioServiceAdapter implements AudioPort {

    private final AudioService audioService;

    @Override
    public String speechToText(byte[] audioData) {
        return audioService.speechToText(audioData);
    }
}

