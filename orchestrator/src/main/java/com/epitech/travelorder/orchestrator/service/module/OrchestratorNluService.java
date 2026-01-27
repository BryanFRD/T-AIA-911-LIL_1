package com.epitech.travelorder.orchestrator.service.module;

import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class OrchestratorNluService {

    private final NluService nluService;

    public Nlu getUnderstandingCustomerPath(String customerRequest) {
        return ofNullable(nluService.understand(customerRequest))
                .orElseThrow(() -> new IncompleteTravelRequestException("NLU could not understand the customer request"));
    }
}
