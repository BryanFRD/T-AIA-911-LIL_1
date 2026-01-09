package com.epitech.travelorder.orchestrator.infrastructure.adapter.out.nlu;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.domain.port.out.NluPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NluServiceAdapter implements NluPort {

    private final NluService nluService;

    @Override
    public Nlu analyze(String text) {
        return nluService.predict(text);
    }
}

