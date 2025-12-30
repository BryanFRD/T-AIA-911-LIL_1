package com.epitech.travelorder.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a loaded ML model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetadata {
    private String modelName;
    private String version;
    private String runId;
}
