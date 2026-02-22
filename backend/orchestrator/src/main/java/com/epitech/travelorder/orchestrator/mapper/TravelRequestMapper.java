package com.epitech.travelorder.orchestrator.mapper;

import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.nlu.model.ner.Ner;
import com.epitech.travelorder.nlu.util.NerUtils;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.mapstruct.factory.Mappers.getMapper;

@Mapper
public interface TravelRequestMapper {

    String FROM = "FROM";
    String TO = "TO";
    String VIA = "VIA";
    String DEPARTURE_TIME = "DEPARTURE_TIME";
    String ARRIVAL_TIME = "ARRIVAL_TIME";

    TravelRequestMapper INSTANCE = getMapper(TravelRequestMapper.class);

    @Mapping(target = "origin", source = ".", qualifiedByName = "extractOrigin")
    @Mapping(target = "destination", source = ".", qualifiedByName = "extractDestination")
    @Mapping(target = "vias", source = ".", qualifiedByName = "extractVias")
    @Mapping(target = "departureTime", source = ".", qualifiedByName = "extractDepartureTime")
    @Mapping(target = "arrivalTime", source = ".", qualifiedByName = "extractArrivalTime")
    RouteQuery convert(Nlu nlu);

    @Named("extractOrigin")
    default String extractOrigin(Nlu nlu) {
        List<String> fromEntities = getEntitiesList(nlu, FROM);
        return fromEntities.isEmpty() ? null : fromEntities.getFirst();
    }

    @Named("extractDestination")
    default String extractDestination(Nlu nlu) {
        List<String> toEntities = getEntitiesList(nlu, TO);
        if (toEntities.isEmpty()) {
            throw new IncompleteTravelRequestException("Cannot extract destination from customer request");
        }
        return toEntities.getFirst();
    }

    @Named("extractVias")
    default List<String> extractVias(Nlu nlu) {
        return getEntitiesList(nlu, VIA);
    }

    @Named("extractDepartureTime")
    default LocalDateTime extractDepartureTime(Nlu nlu) {
        return getEntitiesList(nlu, DEPARTURE_TIME)
                .stream()
                .findFirst()
                .map(NerUtils::extractDateTime)
                .orElse(null);
    }

    @Named("extractArrivalTime")
    default LocalDateTime extractArrivalTime(Nlu nlu) {
        return getEntitiesList(nlu, ARRIVAL_TIME)
                .stream()
                .findFirst()
                .map(NerUtils::extractDateTime)
                .orElse(null);
    }

    private Map<String, List<String>> getEntities(Nlu nlu) {
        return ofNullable(nlu)
                .map(Nlu::ner)
                .map(Ner::entities)
                .map(NerUtils::groupEntitiesByType)
                .orElse(emptyMap());
    }

    private List<String> getEntitiesList(Nlu nlu, String type) {
        return getEntities(nlu).getOrDefault(type, emptyList());
    }
}
