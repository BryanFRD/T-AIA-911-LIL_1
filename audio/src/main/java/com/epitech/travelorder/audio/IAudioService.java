package com.epitech.travelorder.audio;

public interface IAudioService {

    /**
     * Convert speech to text
     * @param audioData Raw audio data
     * @return Transcribed text
     */
    String speechToText(byte[] audioData);

    /**
     * Convert text to speech
     * @param text Text to convert
     * @return Audio data
     */
    byte[] textToSpeech(String text);
}
