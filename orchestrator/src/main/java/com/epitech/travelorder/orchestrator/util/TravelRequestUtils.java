package com.epitech.travelorder.orchestrator.util;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;
import com.epitech.travelorder.common.domain.nlu.ner.NerEntity;
import com.epitech.travelorder.nlu.util.NerUtils;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import com.epitech.travelorder.orchestrator.dto.TravelRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class TravelRequestUtils {

    public static TravelRequest extract(Nlu nlu) {
        List<NerEntity> entities = ofNullable(nlu)
                .map(Nlu::ner)
                .map(Ner::entities)
                .orElse(emptyList());

        Map<String, List<String>> groupedEntities = NerUtils.groupEntitiesByType(entities);

        String origin = getSingleEntityOrThrow(groupedEntities, "FROM", "Origin (FROM) missing");
        String destination = getSingleEntityOrThrow(groupedEntities, "TO", "Destination (TO) missing");

        List<String> viaPoints = groupedEntities.getOrDefault("VIA", emptyList());

        return new TravelRequest(origin, destination, viaPoints);
    }

    private static String getSingleEntityOrThrow(Map<String, List<String>> map, String type, String errorMsg) {
        return map.getOrDefault(type, Collections.emptyList())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IncompleteTravelRequestException(errorMsg));
    }
}

