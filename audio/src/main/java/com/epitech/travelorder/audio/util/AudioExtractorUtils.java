package com.epitech.travelorder.audio.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class AudioExtractorUtils {

    private static final String TEXT_FIELD = "text";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String extractTextFromModelResponse(String json) {
        try {
            byte[] jsonBytes = json.getBytes(UTF_8);
            String utf8Json = new String(jsonBytes, UTF_8);

            JsonNode node = MAPPER.readTree(utf8Json);
            if (node.hasNonNull(TEXT_FIELD)) {
                String text = node.get(TEXT_FIELD).asText();
                return new String(text.getBytes(UTF_8), UTF_8);
            }
            log.warn("The 'text' field is missing in Vosk response");
            return "";
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Vosk JSON response: {}", json, e);
            throw new RuntimeException("Failed to parse Vosk JSON response", e);
        }
    }
}