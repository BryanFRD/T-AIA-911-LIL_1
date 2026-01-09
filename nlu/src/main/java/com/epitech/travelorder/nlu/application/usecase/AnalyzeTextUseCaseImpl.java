package com.epitech.travelorder.nlu.application.usecase;

import com.epitech.travelorder.common.domain.nlu.intent.Intent;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;
import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.nlu.domain.port.in.AnalyzeTextUseCase;
import com.epitech.travelorder.nlu.domain.port.out.IntentPredictorPort;
import com.epitech.travelorder.nlu.domain.port.out.NerPredictorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AnalyzeTextUseCaseImpl implements AnalyzeTextUseCase {

  private static final Log LOG_CTX = Log.of(Log.NLU, "AnalyzeText");

  private final IntentPredictorPort intentPredictor;
  private final NerPredictorPort nerPredictor;

  @Override
  public Nlu analyze(String text) {
    if (text == null || text.isBlank()) {
      log.warn(LOG_CTX.format("Empty or null text provided"));
      return null;
    }

    try {
      log.debug(LOG_CTX.format("Running NLU analysis for text: '%s'", text));

      Intent intent = intentPredictor.predict(text);
      Ner entities = nerPredictor.predict(text);

      Nlu result = new Nlu(intent, entities);
      log.debug(LOG_CTX.format("Analysis completed: %s", result));

      return result;
    } catch (Exception e) {
      log.error(LOG_CTX.format("Prediction failed: %s", e.getMessage()), e);
      return null;
    }
  }
}

