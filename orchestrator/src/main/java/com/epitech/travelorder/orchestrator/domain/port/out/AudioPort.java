package com.epitech.travelorder.orchestrator.domain.port.out;

public interface AudioPort {
    String speechToText(byte[] audioData);
}

