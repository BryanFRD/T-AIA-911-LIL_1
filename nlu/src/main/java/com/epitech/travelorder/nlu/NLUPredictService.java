package com.epitech.travelorder.nlu;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ModelNotFoundException;
import com.epitech.travelorder.common.model.MlFlowConfig;
import com.epitech.travelorder.common.model.ModelLoader;
import com.epitech.travelorder.common.model.ModelMetadata;
import com.epitech.travelorder.common.model.ModelType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * NLU Prediction Service using DJL and MLflow models (Hexagonal Architecture - Adapter)
 */
@Slf4j
public class NLUPredictService implements INLUService {

    @Getter
    private final ModelLoader modelLoader;
    private Predictor<String, IntentResult> intentPredictor;
    private Predictor<String, NERResult> nerPredictor;

    public NLUPredictService(MlFlowConfig config) {
        this.modelLoader = new ModelLoader(config);

        log.info("NLU Prediction Service initialized");
        modelLoader.logAvailableEngines();

        init();
    }

    /**
     * Initialize and load all models
     */
    private void init() {
        log.info("Loading NLU models from MLflow...");
        loadIntentModel();
        loadNERModel();
        log.info("NLU models initialization completed");
    }

    /**
     * Load the Intent Classification model
     */
    private void loadIntentModel() {
        try {
            log.info("Loading Intent Classification model from MLflow...");

            Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_INTENT);

            intentPredictor = modelLoader.createPredictor(
                ModelType.NLU_INTENT,
                new IntentTranslator(artifactsPath),
                String.class,
                IntentResult.class
            );

            log.info("Intent model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load Intent model: {}", e.getMessage(), e);
        }
    }

    /**
     * Load the NER model
     */
    private void loadNERModel() {
        try {
            log.info("Loading NER model from MLflow...");

            Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_NER);

            nerPredictor = modelLoader.createPredictor(
                ModelType.NLU_NER,
                new NERTranslator(artifactsPath),
                String.class,
                NERResult.class
            );

            log.info("NER model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load NER model: {}", e.getMessage(), e);
        }
    }

    @Override
    public NLUResult predict(String text) {
        try {
            log.debug("Running NLU analysis for: {}", text);

            IntentResult intent = intentPredictor.predict(text);

            NERResult entities = nerPredictor.predict(text);

            return new NLUResult(intent, entities);
        } catch (Exception e) {
            log.error("Failed to run NLU prediction", e);
            throw new RuntimeException("NLU prediction failed", e);
        }
    }

    @Override
    public NLUHealthStatus getHealthStatus() {
        List<ModelInfo> models = new ArrayList<>();

        ModelMetadata intentMetadata = modelLoader.getModelMetadata(ModelType.NLU_INTENT);
        if (intentMetadata != null) {
            models.add(new ModelInfo(
                intentMetadata.getModelName(),
                "intent-classification",
                intentMetadata.getVersion(),
                intentMetadata.getRunId(),
                intentPredictor != null
            ));
        }

        ModelMetadata nerMetadata = modelLoader.getModelMetadata(ModelType.NLU_NER);
        if (nerMetadata != null) {
            models.add(new ModelInfo(
                nerMetadata.getModelName(),
                "named-entity-recognition",
                nerMetadata.getVersion(),
                nerMetadata.getRunId(),
                nerPredictor != null
            ));
        }

        String status = (intentPredictor != null && nerPredictor != null) ? "healthy" : "degraded";
        return new NLUHealthStatus(status, models);
    }

    @Override
    public void close() {
        modelLoader.clearCache();
        log.info("NLU Prediction Service closed");
    }
}
