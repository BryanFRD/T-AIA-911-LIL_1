package com.epitech.travelorder.orchestrator.domain.port.out;

import com.epitech.travelorder.common.domain.nlu.Nlu;

public interface NluPort {
    Nlu analyze(String text);
}

