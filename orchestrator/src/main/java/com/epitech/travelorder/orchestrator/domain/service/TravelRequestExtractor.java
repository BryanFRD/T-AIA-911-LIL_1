package com.epitech.travelorder.orchestrator.domain.service;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.domain.nlu.ner.NerEntity;
import com.epitech.travelorder.common.util.NerUtils;
import com.epitech.travelorder.orchestrator.domain.exception.business.IncompleteTravelRequestException;
import com.epitech.travelorder.orchestrator.domain.model.TravelRequest;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class TravelRequestExtractor {

    public TravelRequest extract(Nlu nlu) {
        List<NerEntity> entities = (nlu.ner() != null) ? emptyIfNull(nlu.ner().entities()) : emptyList();

        Map<String, List<String>> groupedEntities = NerUtils.groupEntitiesByType(entities);

        String origin = getFirstOrThrow(groupedEntities, "FROM", "Origin (FROM) missing in request");
        String destination = getFirstOrThrow(groupedEntities, "TO", "Destination (TO) missing in request");
        List<String> viaPoints = groupedEntities.getOrDefault("VIA", emptyList());

        return new TravelRequest(origin, destination, viaPoints);
    }

    private String getFirstOrThrow(Map<String, List<String>> entities, String type, String errorMessage) {
        return entities.getOrDefault(type, emptyList())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IncompleteTravelRequestException(errorMessage));
    }
}

