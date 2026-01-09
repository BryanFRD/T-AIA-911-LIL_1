package com.epitech.travelorder.common.util;

import com.epitech.travelorder.common.domain.enumeration.NerLabel;
import com.epitech.travelorder.common.domain.nlu.ner.NerEntity;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class NerUtils {

    /**
     * Reconstructs full entities from BIO-tagged tokens using Java 21 features.
     * Example: [B-TO("New"), I-TO("York")] -> Map{"TO": ["New York"]}
     */
    public static Map<String, List<String>> groupEntitiesByType(List<NerEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyMap();
        }

        var extractedData = new HashMap<String, List<String>>();
        var currentBuffer = new StringBuilder();
        NerLabel currentType = null;

        for (var entity : entities) {
            var label = entity.nerLabel();

            boolean isContinuation = currentType != null
                    && label.isInside()
                    && label.entityType().equals(currentType.entityType());

            if (isContinuation) {
                currentBuffer.append(" ").append(entity.text());
            } else {
                // Flush previous entity
                if (currentType != null) {
                    addEntityToMap(extractedData, currentType.entityType(), currentBuffer.toString());
                }

                // Start new entity or reset
                if (label.isBegin()) {
                    currentBuffer.setLength(0);
                    currentBuffer.append(entity.text());
                    currentType = label;
                } else {
                    currentBuffer.setLength(0);
                    currentType = null;
                }
            }
        }

        if (currentType != null) {
            addEntityToMap(extractedData, currentType.entityType(), currentBuffer.toString());
        }

        return extractedData;
    }

    private static void addEntityToMap(Map<String, List<String>> map, String type, String value) {
        map.computeIfAbsent(type, k -> new ArrayList<>()).add(value);
    }
}