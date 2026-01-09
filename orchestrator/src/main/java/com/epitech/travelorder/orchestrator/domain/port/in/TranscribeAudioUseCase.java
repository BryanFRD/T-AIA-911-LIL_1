package com.epitech.travelorder.orchestrator.domain.port.in;

public interface TranscribeAudioUseCase {
    String transcribe(byte[] audioData);
}


