package com.epitech.travelorder.nlu.infrastructure.adapter.djl;

import ai.djl.inference.Predictor;
import com.epitech.travelorder.common.domain.mlflow.ModelLoader;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;
import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.nlu.domain.port.out.NerPredictorPort;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class NerPredictorAdapter implements NerPredictorPort {

  private static final Log LOG_CTX = Log.of(Log.NLU, "NerPredictor");

  private final Predictor<String, Ner> predictor;

  public NerPredictorAdapter(ModelLoader modelLoader) {
    try {
      log.info(LOG_CTX.format("Loading NER model"));

      Path artifactsPath = modelLoader.ensureArtifactsDownloaded(ModelType.NLU_NER);

      this.predictor = modelLoader.createPredictor(
          ModelType.NLU_NER,
          new NerTranslator(artifactsPath),
          String.class,
          Ner.class
      );

      log.info(LOG_CTX.format("NER model loaded successfully"));
    } catch (Exception e) {
      log.error(LOG_CTX.format("Failed to load NER model: %s", e.getMessage()), e);
      throw new RuntimeException("Failed to initialize NER predictor", e);
    }
  }

  @Override
  public Ner predict(String text) {
    if (predictor == null) {
      log.error(LOG_CTX.format("Predictor not initialized"));
      return null;
    }

    try {
      return predictor.predict(text);
    } catch (Exception e) {
      log.error(LOG_CTX.format("NER prediction failed: %s", e.getMessage()), e);
      return null;
    }
  }
}

