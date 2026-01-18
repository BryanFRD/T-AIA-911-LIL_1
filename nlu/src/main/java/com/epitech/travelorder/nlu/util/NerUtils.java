package com.epitech.travelorder.nlu.util;

import com.epitech.travelorder.common.domain.enumeration.NerLabel;
import com.epitech.travelorder.common.domain.nlu.ner.NerEntity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@UtilityClass
public class NerUtils {

    public static Map<String, List<String>> groupEntitiesByType(List<NerEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> extractedData = new HashMap<>();
        StringBuilder currentBuffer = new StringBuilder();
        String currentEntityType = null;
        String previousText = "";

        for (NerEntity entity : entities) {
            NerLabel label = entity.nerLabel();
            String text = entity.text();

            if (label.isOutside()) {
                flushEntity(extractedData, currentEntityType, currentBuffer);
                currentEntityType = null;
                previousText = text;
                continue;
            }

            String entityType = label.entityType();

            boolean isContinuation = currentEntityType != null && currentEntityType.equals(entityType);

            if (isContinuation) {
                if (shouldAppendWithoutSpace(previousText, text)) {
                    currentBuffer.append(text);
                } else {
                    currentBuffer.append(" ").append(text);
                }
            } else {
                flushEntity(extractedData, currentEntityType, currentBuffer);
                currentEntityType = entityType;
                currentBuffer.append(text);
            }

            previousText = text;
        }

        flushEntity(extractedData, currentEntityType, currentBuffer);
        return extractedData;
    }

    private static void flushEntity(Map<String, List<String>> map, String type, StringBuilder buffer) {
        if (type != null && !buffer.isEmpty()) {
            String value = buffer.toString();
            log.debug("Extracted entity: type={}, value='{}'", type, value);
            map.computeIfAbsent(type, k -> new ArrayList<>()).add(value);
            buffer.setLength(0);
        }
    }

    private static boolean shouldAppendWithoutSpace(String previous, String current) {
        return previous.endsWith("-") || current.startsWith("-");
    }
}