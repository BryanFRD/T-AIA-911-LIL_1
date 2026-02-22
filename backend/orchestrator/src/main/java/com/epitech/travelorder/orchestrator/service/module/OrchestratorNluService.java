package com.epitech.travelorder.orchestrator.service.module;

import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class OrchestratorNluService {

    private final NluService nluService;

    public Nlu getUnderstandingCustomerPath(String customerRequest) {
        String capitalizedRequest = capitalizeCustomerRequestForCompatibility(customerRequest);
        return ofNullable(nluService.understand(capitalizedRequest))
                .orElseThrow(() -> new IncompleteTravelRequestException("NLU could not understand the customer request"));
    }

    private String capitalizeCustomerRequestForCompatibility(String customerRequest) {
        return Arrays.stream(customerRequest.split("\\s+"))
                .map(this::capitalizeWord)
                .collect(Collectors.joining(" "));
    }

    private String capitalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }

        if (word.length() == 1) {
            return word.toUpperCase();
        }

        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
}
