package com.epitech.travelorder.nlu.domain.port.out;

import com.epitech.travelorder.common.domain.nlu.intent.Intent;

public interface IntentPredictorPort {
  Intent predict(String text);
}

