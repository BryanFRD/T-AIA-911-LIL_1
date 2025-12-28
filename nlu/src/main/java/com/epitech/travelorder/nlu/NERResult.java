package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of Named Entity Recognition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NERResult {
    private List<Entity> entities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entity {
        private String text;
        private String label;
        private float confidence;
        private int start;
        private int end;

        @Override
        public String toString() {
            return String.format("%s (%s, %.2f%%)", text, label, confidence * 100);
        }
    }

    @Override
    public String toString() {
        if (entities == null || entities.isEmpty()) {
            return "No entities found";
        }
        return "Entities: " + entities.toString();
    }
}
