package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Health status information for NLU service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NLUHealthStatus {

    private String status;
    private List<ModelInfo> models;
}
