package com.epitech.travelorder.nlu;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ModelNotFoundException;
import com.epitech.travelorder.common.ml.MLflowConfig;
import com.epitech.travelorder.common.ml.ModelLoader;
import com.epitech.travelorder.common.ml.ModelType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * NLU Prediction Service using DJL and MLflow models (Hexagonal Architecture - Adapter)
 */
@Slf4j
public class NLUPredictService implements INLUService {

    private final ModelLoader modelLoader;
    private Predictor<String, IntentResult> intentPredictor;
    private Predictor<String, NERResult> nerPredictor;

    public NLUPredictService(MLflowConfig config) {
        this.modelLoader = new ModelLoader(config);

        log.info("NLU Prediction Service initialized");
        modelLoader.logAvailableEngines();

        // Load models at construction
        init();
    }

    /**
     * Initialize and load all models
     */
    private void init() {
        try {
            log.info("Loading NLU models from MLflow...");
            loadIntentModel();
            loadNERModel();
            log.info("All NLU models loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load NLU models at startup", e);
            throw new RuntimeException("Failed to initialize NLU models", e);
        }
    }

    /**
     * Load the Intent Classification model
     */
    private void loadIntentModel() throws ModelNotFoundException, IOException, MalformedModelException {
        log.info("Loading Intent Classification model from MLflow...");

        intentPredictor = modelLoader.createPredictor(
            ModelType.NLU_INTENT,
            new IntentTranslator(),
            String.class,
            IntentResult.class
        );

        log.info("Intent model loaded successfully");
    }

    /**
     * Load the NER model
     */
    private void loadNERModel() throws ModelNotFoundException, IOException, MalformedModelException {
        log.info("Loading NER model from MLflow...");

        nerPredictor = modelLoader.createPredictor(
            ModelType.NLU_NER,
            new NERTranslator(),
            String.class,
            NERResult.class
        );

        log.info("NER model loaded successfully");
    }

    @Override
    public NLUResult predict(String text) {
        try {
            log.debug("Running NLU analysis for: {}", text);

            // Predict intent
            IntentResult intent = intentPredictor.predict(text);

            // Extract entities
            NERResult entities = nerPredictor.predict(text);

            return new NLUResult(intent, entities);
        } catch (Exception e) {
            log.error("Failed to run NLU prediction", e);
            throw new RuntimeException("NLU prediction failed", e);
        }
    }

    @Override
    public void close() {
        modelLoader.clearCache();
        log.info("NLU Prediction Service closed");
    }
}
