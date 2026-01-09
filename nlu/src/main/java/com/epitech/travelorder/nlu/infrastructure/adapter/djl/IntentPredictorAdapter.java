package com.epitech.travelorder.nlu.infrastructure.adapter.djl;

import ai.djl.inference.Predictor;
import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.domain.nlu.intent.Intent;
import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.nlu.domain.port.out.IntentPredictorPort;
import com.epitech.travelorder.nlu.translator.IntentTranslator;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class IntentPredictorAdapter implements IntentPredictorPort {

  private static final Log LOG_CTX = Log.of(Log.NLU, "IntentPredictor");

  private final Predictor<String, Intent> predictor;

  public IntentPredictorAdapter(ModelLoader modelLoader) {
    try {
      log.info(LOG_CTX.format("Loading Intent Classification model"));

      Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_INTENT);

      this.predictor = modelLoader.createPredictor(
          ModelType.NLU_INTENT,
          new IntentTranslator(artifactsPath),
          String.class,
          Intent.class
      );

      log.info(LOG_CTX.format("Intent model loaded successfully"));
    } catch (Exception e) {
      log.error(LOG_CTX.format("Failed to load Intent model: %s", e.getMessage()), e);
      throw new RuntimeException("Failed to initialize Intent predictor", e);
    }
  }

  @Override
  public Intent predict(String text) {
    if (predictor == null) {
      log.error(LOG_CTX.format("Predictor not initialized"));
      return null;
    }

    try {
      return predictor.predict(text);
    } catch (Exception e) {
      log.error(LOG_CTX.format("Intent prediction failed: %s", e.getMessage()), e);
      return null;
    }
  }
}


