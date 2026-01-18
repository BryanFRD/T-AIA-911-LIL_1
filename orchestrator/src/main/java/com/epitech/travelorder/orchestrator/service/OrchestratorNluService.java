package com.epitech.travelorder.orchestrator.service;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import com.epitech.travelorder.orchestrator.config.exception.technical.InternalServiceDown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class OrchestratorNluService {

    private final NluService nluService;

    public Nlu analyze(String text) {
        return ofNullable(nluService.analyze(text))
                .orElseThrow(() -> new IncompleteTravelRequestException("Failed to analyze the text for NLU."));
    }

}
