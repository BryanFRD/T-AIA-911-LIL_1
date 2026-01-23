package com.epitech.travelorder.orchestrator.mapper;

import com.epitech.travelorder.common.domain.nlu.Nlu;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;
import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.nlu.util.NerUtils;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.mapstruct.factory.Mappers.getMapper;

@Mapper
public interface TravelRequestMapper {

    String FROM = "FROM";
    String TO = "TO";
    String VIA = "VIA";
    String DATETIME = "DATETIME";

    TravelRequestMapper INSTANCE = getMapper(TravelRequestMapper.class);

    @Mapping(target = "origin", source = ".", qualifiedByName = "extractOrigin")
    @Mapping(target = "destination", source = ".", qualifiedByName = "extractDestination")
    @Mapping(target = "vias", source = ".", qualifiedByName = "extractVias")
    @Mapping(target = "departureTime", source = ".", qualifiedByName = "extractDepartureTime")
    public RouteQuery convert(Nlu nlu);

    @Named("extractOrigin")
    default String extractOrigin(Nlu nlu) {
        return findEntity(nlu, FROM)
                .orElse(null);
    }

    @Named("extractDestination")
    default String extractDestination(Nlu nlu) {
        return findEntity(nlu, TO)
                .orElseThrow(() -> new IncompleteTravelRequestException("Cannot extract destination from customer request"));
    }

    @Named("extractVias")
    default List<String> extractVias(Nlu nlu) {
        return getEntities(nlu)
                .getOrDefault(VIA, emptyList());
    }

    @Named("extractDepartureTime")
    default LocalDateTime extractDepartureTime(Nlu nlu) {
        return findEntity(nlu, DATETIME)
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

    private Optional<String> findEntity(Nlu nlu, String type) {
        return getEntities(nlu)
                .getOrDefault(type, emptyList())
                .stream()
                .findFirst();
    }
}
