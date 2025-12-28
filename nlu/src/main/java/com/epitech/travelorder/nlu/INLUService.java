package com.epitech.travelorder.nlu;

/**
 * Interface for NLU service (Hexagonal Architecture - Port)
 */
public interface INLUService {

    /**
     * Perform complete NLU analysis on text (intent + entities)
     *
     * @param text Input text
     * @return Combined NLU result with intent and entities
     */
    NLUResult predict(String text);

    /**
     * Clean up resources
     */
    void close();
}
