package com.epitech.travelorder.nlu.service;

import ai.djl.inference.Predictor;
import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.common.domain.mlflow.ModelMetadata;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.domain.nlu.NluModelInfo;
import com.epitech.travelorder.common.domain.nlu.NluStatus;
import com.epitech.travelorder.common.domain.nlu.intent.Intent;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;
import com.epitech.travelorder.common.util.LogUtils;
import com.epitech.travelorder.nlu.translator.IntentTranslator;
import com.epitech.travelorder.nlu.translator.NerTranslator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.epitech.travelorder.common.util.LogUtils.NLU;

@Slf4j
@Service
public class NluService {

    private static final LogUtils LOG_CTX_INIT = LogUtils.of(NLU, "Init");
    private static final LogUtils LOG_CTX_MODEL = LogUtils.of(NLU, "ModelLoad");
    private static final LogUtils LOG_CTX_PREDICT = LogUtils.of(NLU, "Predict");
    private static final LogUtils LOG_CTX_HEALTH = LogUtils.of(NLU, "Health");

    @Getter
    private final ModelLoader modelLoader;

    private Predictor<String, Intent> intentPredictor;
    private Predictor<String, Ner> nerPredictor;

    public NluService(ModelLoader modelLoader) {
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
                    Intent.class
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
                    Ner.class
            );

            log.info(LOG_CTX_MODEL.format("NER model loaded successfully"));
        } catch (Exception e) {
            log.error(LOG_CTX_MODEL.format("Failed to load NER model: %s", e.getMessage()), e);
        }
    }

    public Nlu understand(String text) {
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

            Intent intent = intentPredictor.predict(text);
            Ner entities = nerPredictor.predict(text);

            Nlu result = new Nlu(intent, entities);
            log.debug(LOG_CTX_PREDICT.format("Analysis completed: %s", result));

            return result;
        } catch (Exception e) {
            log.error(LOG_CTX_PREDICT.format("Prediction failed: %s", e.getMessage()), e);
            return null;
        }
    }

    public NluStatus healthStatus() {
        List<NluModelInfo> models = new ArrayList<>();

        ModelMetadata intentMetadata = modelLoader.getModelMetadata(ModelType.NLU_INTENT);
        if (intentMetadata != null) {
            models.add(new NluModelInfo(
                    intentMetadata.modelName(),
                    "intentLabel-classification",
                    intentMetadata.version(),
                    intentMetadata.runId(),
                    intentPredictor != null
            ));
        }

        ModelMetadata nerMetadata = modelLoader.getModelMetadata(ModelType.NLU_NER);
        if (nerMetadata != null) {
            models.add(new NluModelInfo(
                    nerMetadata.modelName(),
                    "named-entity-recognition",
                    nerMetadata.version(),
                    nerMetadata.runId(),
                    nerPredictor != null
            ));
        }

        String status = (intentPredictor != null && nerPredictor != null) ? "healthy" : "degraded";
        log.debug(LOG_CTX_HEALTH.format("Health check: %s (%s models)", status, models.size()));

        return new NluStatus(status, models);
    }

    public void close() {
        log.info(LOG_CTX_INIT.format("Closing service and clearing model cache"));
        modelLoader.clearCache();
    }
}

