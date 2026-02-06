package com.epitech.travelorder.audio.util;

import com.epitech.travelorder.audio.bo.TranscriptionBo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static lombok.AccessLevel.PRIVATE;
import static org.hibernate.type.descriptor.java.IntegerJavaType.ZERO;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class AudioExtractorUtils {

    private static final String TEXT_FIELD = "text";
    private static final String RESULT_FIELD = "result";
    private static final String CONFIDENCE_FIELD = "conf";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TranscriptionBo extractTranscriptionBoFromModelResponse(String jsonResponse) {
        String transcribedText = extractTranscribedText(jsonResponse);
        double confidence = extractConfidenceScore(jsonResponse);
        return new TranscriptionBo(transcribedText, confidence);
    }

    private static String extractTranscribedText(String json) {
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

    private static double extractConfidenceScore(String jsonResponse) {
        try {
            JsonNode root = MAPPER.readTree(jsonResponse);

            if (!root.has(RESULT_FIELD)) {
                return ZERO;
            }

            JsonNode results = root.get(RESULT_FIELD);
            if (!results.isArray() || results.isEmpty()) {
                return ZERO;
            }

            double confidence = 0.0;
            int count = 0;

            for (JsonNode word: results) {
                if (word.has(CONFIDENCE_FIELD)) {
                    confidence += word.get(CONFIDENCE_FIELD).asDouble();
                    count++;
                }
            }

            return count > 0 ? confidence / count : ZERO;
        } catch (JsonMappingException e) {
            log.error("Invalid JSON structure in Vosk response: {}", jsonResponse, e);
            throw new RuntimeException("Invalid JSON structure in Vosk response", e);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Vosk JSON response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse Vosk JSON response", e);
        }
    }
}