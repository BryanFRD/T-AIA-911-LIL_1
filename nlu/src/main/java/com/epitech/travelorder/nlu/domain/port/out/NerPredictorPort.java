package com.epitech.travelorder.nlu.domain.port.out;

import com.epitech.travelorder.common.domain.nlu.ner.Ner;

public interface NerPredictorPort {
  Ner predict(String text);
}

