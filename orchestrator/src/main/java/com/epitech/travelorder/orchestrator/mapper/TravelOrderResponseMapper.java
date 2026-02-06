package com.epitech.travelorder.orchestrator.mapper;

import com.epitech.travelorder.audio.bo.TranscriptionBo;
import com.epitech.travelorder.orchestrator.controller.dto.TranscribeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import static org.mapstruct.factory.Mappers.getMapper;

@Mapper
public interface TravelOrderResponseMapper {

    TravelOrderResponseMapper INSTANCE = getMapper(TravelOrderResponseMapper.class);

    @Mapping(source = ".", target = "transcription")
    @Mapping(source = ".", target = "canSkipUserConfirmation", qualifiedByName = "deriveCanSkipUserConfirmation")
    TranscribeResponse convert(TranscriptionBo source);

    @Named("deriveCanSkipUserConfirmation")
    default boolean deriveCanSkipUserConfirmation(TranscriptionBo transcriptionBo) {
        return transcriptionBo.isReliable();
    }
}


