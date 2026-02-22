package com.epitech.travelorder.orchestrator.config.health;

import com.epitech.travelorder.common.nlu.ModelLoader;
import com.epitech.travelorder.common.nlu.mlflow.ModelMetadata;
import com.epitech.travelorder.common.nlu.mlflow.ModelStatus;
import com.epitech.travelorder.common.enumeration.ModelType;
import com.epitech.travelorder.common.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.epitech.travelorder.common.enumeration.LoadStatus.FAILED;
import static com.epitech.travelorder.common.enumeration.LoadStatus.READY;
import static com.epitech.travelorder.common.util.LogUtils.ORCHESTRATOR;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelsHealthIndicator implements HealthIndicator {

    private static final LogUtils LOG_CTX = LogUtils.of(ORCHESTRATOR, "ModelsHealth");

    private final ModelLoader modelLoader;

    @Override
    public Health health() {
        Map<ModelType, ModelStatus> modelStatuses = modelLoader.getAllModelStatuses();

        Map<String, Object> details = new HashMap<>();
        int readyCount = 0;
        int failedCount = 0;
        int totalCount = modelStatuses.size();

        for (Map.Entry<ModelType, ModelStatus> entry : modelStatuses.entrySet()) {
            ModelType modelType = entry.getKey();
            ModelStatus modelStatus = entry.getValue();

            details.put(modelType.getDescription(), buildModelInfo(modelStatus));

            if (READY.equals(modelStatus.status())) {
                readyCount++;
            } else if (FAILED.equals(modelStatus.status())) {
                failedCount++;
            }
        }

        details.put("summary", Map.of(
                "total", totalCount,
                "ready", readyCount,
                "failed", failedCount
        ));

        Health health = determineHealth(readyCount, totalCount, details);

        log.debug(LOG_CTX.format("Health check: %s (%d/%d models ready)",
                health.getStatus(), readyCount, totalCount));

        return health;
    }

    private Map<String, Object> buildModelInfo(ModelStatus modelStatus) {
        Map<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("status", modelStatus.status());
        modelInfo.put("message", modelStatus.message());

        if (modelStatus.metadata() != null) {
            ModelMetadata metadata = modelStatus.metadata();
            modelInfo.put("version", metadata.version());
            modelInfo.put("modelName", metadata.modelName());
            modelInfo.put("runId", metadata.runId());
        }

        if (modelStatus.lastAttempt() != null) {
            modelInfo.put("lastAttempt", modelStatus.lastAttempt());
        }

        return modelInfo;
    }

    private Health determineHealth(int readyCount, int totalCount, Map<String, Object> details) {
        if (readyCount == 0) {
            return Health.down().withDetails(details).build();
        } else if (readyCount < totalCount) {
            return Health.status("PARTIAL").withDetails(details).build();
        } else {
            return Health.up().withDetails(details).build();
        }
    }
}
