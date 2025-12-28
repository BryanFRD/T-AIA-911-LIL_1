package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a loaded ML model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    private String name;
    private String type;
    private String version;
    private String runId;
    private boolean loaded;
}
