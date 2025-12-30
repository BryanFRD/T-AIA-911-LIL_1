package com.epitech.travelorder.orchestrator.health;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.audio.IAudioService;
import com.epitech.travelorder.common.model.ModelStatus;
import com.epitech.travelorder.common.model.ModelType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModelsHealthIndicator implements HealthIndicator {

    private final IAudioService audioService;

    @Override
    public Health health() {
        Map<ModelType, ModelStatus> modelStatuses = ((AudioService) audioService).getModelLoader().getAllModelStatuses();

        Map<String, Object> details = new HashMap<>();
        boolean allReady = true;
        int readyCount = 0;
        int totalCount = modelStatuses.size();

        for (Map.Entry<ModelType, ModelStatus> entry : modelStatuses.entrySet()) {
            ModelType modelType = entry.getKey();
            ModelStatus status = entry.getValue();

            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("status", status.getStatus().toString());
            modelInfo.put("message", status.getMessage());

            if (status.getMetadata() != null) {
                modelInfo.put("version", status.getMetadata().getVersion());
                modelInfo.put("modelName", status.getMetadata().getModelName());
                modelInfo.put("runId", status.getMetadata().getRunId());
            }

            if (status.getLastAttempt() != null) {
                modelInfo.put("lastAttempt", status.getLastAttempt().toString());
            }

            details.put(modelType.getDescription(), modelInfo);

            if (status.getStatus() == ModelStatus.LoadStatus.READY) {
                readyCount++;
            } else if (status.getStatus() != ModelStatus.LoadStatus.NOT_LOADED) {
                allReady = false;
            }
        }

        details.put("summary", Map.of(
                "total", totalCount,
                "ready", readyCount,
                "failed", (int) modelStatuses.values().stream()
                        .filter(s -> s.getStatus() == ModelStatus.LoadStatus.FAILED)
                        .count()
        ));

        if (readyCount == 0) {
            return Health.down()
                    .withDetails(details)
                    .build();
        } else if (!allReady || readyCount < totalCount) {
            return Health.status("PARTIAL")
                    .withDetails(details)
                    .build();
        } else {
            return Health.up()
                    .withDetails(details)
                    .build();
        }
    }
}
