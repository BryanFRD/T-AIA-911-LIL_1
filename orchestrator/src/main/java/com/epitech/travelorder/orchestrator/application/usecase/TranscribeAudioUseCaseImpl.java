package com.epitech.travelorder.orchestrator.application.usecase;

import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.orchestrator.domain.exception.technical.AudioProcessingException;
import com.epitech.travelorder.orchestrator.domain.port.in.TranscribeAudioUseCase;
import com.epitech.travelorder.orchestrator.domain.port.out.AudioPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TranscribeAudioUseCaseImpl implements TranscribeAudioUseCase {

  private static final Log LOG_CTX = Log.of(Log.ORCH, "TranscribeAudio");

  private final AudioPort audioPort;

  @Override
  public String transcribe(byte[] audioData) {
    try {
      log.info(LOG_CTX.format("Transcribing audio data (%s bytes)", audioData.length));
      String transcribedText = audioPort.speechToText(audioData);
      log.info(LOG_CTX.format("Audio transcription completed"));
      return transcribedText;
    } catch (Exception e) {
      log.error(LOG_CTX.format("Failed to transcribe audio"), e);
      throw new AudioProcessingException("Audio transcription failed", e);
    }
  }
}
