package com.epitech.travelorder.nlu.domain.port.in;

import com.epitech.travelorder.common.domain.nlu.Nlu;

public interface AnalyzeTextUseCase {
    Nlu analyze(String text);
}


