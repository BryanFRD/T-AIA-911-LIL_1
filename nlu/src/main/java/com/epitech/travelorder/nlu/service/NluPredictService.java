package com.epitech.travelorder.nlu.service;

import ai.djl.inference.Predictor;
import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.common.model.ModelMetadata;
import com.epitech.travelorder.common.model.ModelType;
import com.epitech.travelorder.common.util.LogContext;
import com.epitech.travelorder.nlu.dto.IntentResult;
import com.epitech.travelorder.nlu.dto.ModelInfo;
import com.epitech.travelorder.nlu.dto.NerResult;
import com.epitech.travelorder.nlu.dto.NluHealthStatus;
import com.epitech.travelorder.nlu.dto.NluResult;
import com.epitech.travelorder.nlu.translator.IntentTranslator;
import com.epitech.travelorder.nlu.translator.NerTranslator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NluPredictService {

    private static final LogContext LOG_CTX_INIT = LogContext.of(LogContext.NLU, "Init");

    private static final LogContext LOG_CTX_MODEL = LogContext.of(LogContext.NLU, "ModelLoad");

    private static final LogContext LOG_CTX_PREDICT = LogContext.of(LogContext.NLU, "Predict");

    private static final LogContext LOG_CTX_HEALTH = LogContext.of(LogContext.NLU, "Health");

    @Getter
    private final ModelLoader modelLoader;

    private Predictor<String, IntentResult> intentPredictor;

    private Predictor<String, NerResult> nerPredictor;

    public NluPredictService(ModelLoader modelLoader) {
        this.modelLoader = modelLoader;
        log.info(LOG_CTX_INIT.format("Service initialization started"));
        modelLoader.logAvailableEngines();
        loadModels();
        log.info(LOG_CTX_INIT.format("Service initialization completed"));
    }

    private void loadModels() {
        loadIntentModel();
        loadNerModel();
    }

    private void loadIntentModel() {
        try {
            log.info(LOG_CTX_MODEL.format("Loading Intent Classification model"));

            Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_INTENT);

            intentPredictor = modelLoader.createPredictor(
                    ModelType.NLU_INTENT,
                    new IntentTranslator(artifactsPath),
                    String.class,
                    IntentResult.class
            );

            log.info(LOG_CTX_MODEL.format("Intent model loaded successfully"));
        } catch (Exception e) {
            log.error(LOG_CTX_MODEL.format("Failed to load Intent model: %s", e.getMessage()), e);
        }
    }

    private void loadNerModel() {
        try {
            log.info(LOG_CTX_MODEL.format("Loading NER model"));

            Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_NER);

            nerPredictor = modelLoader.createPredictor(
                    ModelType.NLU_NER,
                    new NerTranslator(artifactsPath),
                    String.class,
                    NerResult.class
            );

            log.info(LOG_CTX_MODEL.format("NER model loaded successfully"));
        } catch (Exception e) {
            log.error(LOG_CTX_MODEL.format("Failed to load NER model: %s", e.getMessage()), e);
        }
    }

    public NluResult predict(String text) {
        if (text == null || text.isBlank()) {
            log.warn(LOG_CTX_PREDICT.format("Empty or null text provided"));
            return null;
        }

        if (intentPredictor == null || nerPredictor == null) {
            log.error(LOG_CTX_PREDICT.format("Models not loaded, cannot predict"));
            return null;
        }

        try {
            log.debug(LOG_CTX_PREDICT.format("Running NLU analysis for text: '%s'", text));

            IntentResult intent = intentPredictor.predict(text);
            NerResult entities = nerPredictor.predict(text);

            NluResult result = new NluResult(intent, entities);
            log.debug(LOG_CTX_PREDICT.format("Analysis completed: %s", result));

            return result;
        } catch (Exception e) {
            log.error(LOG_CTX_PREDICT.format("Prediction failed: %s", e.getMessage()), e);
            return null;
        }
    }

    public NluHealthStatus healthStatus() {
        List<ModelInfo> models = new ArrayList<>();

        ModelMetadata intentMetadata = modelLoader.getModelMetadata(ModelType.NLU_INTENT);
        if (intentMetadata != null) {
            models.add(new ModelInfo(
                    intentMetadata.modelName(),
                    "intent-classification",
                    intentMetadata.version(),
                    intentMetadata.runId(),
                    intentPredictor != null
            ));
        }

        ModelMetadata nerMetadata = modelLoader.getModelMetadata(ModelType.NLU_NER);
        if (nerMetadata != null) {
            models.add(new ModelInfo(
                    nerMetadata.modelName(),
                    "named-entity-recognition",
                    nerMetadata.version(),
                    nerMetadata.runId(),
                    nerPredictor != null
            ));
        }

        String status = (intentPredictor != null && nerPredictor != null) ? "healthy" : "degraded";
        log.debug(LOG_CTX_HEALTH.format("Health check: %s (%d models)", status, models.size()));

        return new NluHealthStatus(status, models);
    }

    public void close() {
        log.info(LOG_CTX_INIT.format("Closing service and clearing model cache"));
        modelLoader.clearCache();
    }
}

