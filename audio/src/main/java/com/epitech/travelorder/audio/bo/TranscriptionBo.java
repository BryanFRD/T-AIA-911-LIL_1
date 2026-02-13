package com.epitech.travelorder.audio.bo;

public record TranscriptionBo(
        String transcribedText,
        double confidence,
        boolean isReliable
) {

    private static final double RELIABILITY_THRESHOLD = 0.90;

    public TranscriptionBo(String transcribedText, double confidence) {
        this(transcribedText, confidence, confidence >= RELIABILITY_THRESHOLD);
    }

}
