package com.epitech.travelorder.audio.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class AudioExtractorUtils {

    private static final String TEXT_FIELD = "text";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String extractTextFromModelResponse(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node.hasNonNull(TEXT_FIELD)) {
                return node.get(TEXT_FIELD).asText();
            }
            log.warn("The 'text' field is missing in Vosk response");
            return "";
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Vosk JSON response: {}", json, e);
            throw new RuntimeException("Failed to parse Vosk JSON response", e);
        }
    }
}