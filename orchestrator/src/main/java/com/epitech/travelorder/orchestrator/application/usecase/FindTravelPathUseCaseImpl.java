package com.epitech.travelorder.orchestrator.application.usecase;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.infrastructure.Log;
import com.epitech.travelorder.orchestrator.domain.exception.technical.InternalServiceDown;
import com.epitech.travelorder.orchestrator.domain.model.TravelRequest;
import com.epitech.travelorder.orchestrator.domain.port.in.FindTravelPathUseCase;
import com.epitech.travelorder.orchestrator.domain.port.out.NluPort;
import com.epitech.travelorder.orchestrator.domain.port.out.PathfinderPort;
import com.epitech.travelorder.orchestrator.domain.service.TravelRequestExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@RequiredArgsConstructor
public class FindTravelPathUseCaseImpl implements FindTravelPathUseCase {

    private static final Log LOG_CTX = Log.of(Log.ORCH, "FindTravelPath");

    private final NluPort nluPort;

    private final PathfinderPort pathfinderPort;

    private final TravelRequestExtractor travelRequestExtractor;

    @Override
    public String findPath(String customerRequest) {
        Nlu nlu = nluPort.analyze(customerRequest);
        if (nlu == null) {
            throw new InternalServiceDown("Fail to analyze customer request");
        }

        TravelRequest travelRequest = travelRequestExtractor.extract(nlu);
        if (isEmpty(travelRequest.viaPoints())) {
            return pathfinderPort.findRoute(travelRequest.origin(), travelRequest.destination());
        }

        return pathfinderPort.findRouteWithVia(travelRequest.origin(), travelRequest.destination(), travelRequest.viaPoints());
    }
}

