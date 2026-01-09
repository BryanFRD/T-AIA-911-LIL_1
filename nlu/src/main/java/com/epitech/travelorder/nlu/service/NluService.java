package com.epitech.travelorder.nlu.service;

import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.common.domain.mlflow.ModelMetadata;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.domain.nlu.NluModelInfo;
import com.epitech.travelorder.common.domain.nlu.NluStatus;
import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.nlu.application.usecase.AnalyzeTextUseCaseImpl;
import com.epitech.travelorder.nlu.domain.port.in.AnalyzeTextUseCase;
import com.epitech.travelorder.nlu.domain.port.out.IntentPredictorPort;
import com.epitech.travelorder.nlu.domain.port.out.NerPredictorPort;
import com.epitech.travelorder.nlu.infrastructure.adapter.djl.IntentPredictorAdapter;
import com.epitech.travelorder.nlu.infrastructure.adapter.djl.NerPredictorAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NluService {

  private static final Log LOG_CTX_INIT = Log.of(Log.NLU, "Init");
  private static final Log LOG_CTX_HEALTH = Log.of(Log.NLU, "Health");

  @Getter
  private final ModelLoader modelLoader;

  private final AnalyzeTextUseCase analyzeTextUseCase;
  private IntentPredictorPort intentPredictor;
  private NerPredictorPort nerPredictor;

  public NluService(ModelLoader modelLoader) {
    this.modelLoader = modelLoader;
    log.info(LOG_CTX_INIT.format("Service initialization started"));
    modelLoader.logAvailableEngines();
    loadModels();
    log.info(LOG_CTX_INIT.format("Service initialization completed"));

    this.analyzeTextUseCase = new AnalyzeTextUseCaseImpl(intentPredictor, nerPredictor);
  }

  private void loadModels() {
    try {
      intentPredictor = new IntentPredictorAdapter(modelLoader);
      nerPredictor = new NerPredictorAdapter(modelLoader);
    } catch (Exception e) {
      log.error(LOG_CTX_INIT.format("Failed to load models: %s", e.getMessage()), e);
    }
  }

  public Nlu predict(String text) {
    return analyzeTextUseCase.analyze(text);
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

