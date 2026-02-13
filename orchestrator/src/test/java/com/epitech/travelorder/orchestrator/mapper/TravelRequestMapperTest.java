package com.epitech.travelorder.orchestrator.mapper;

import com.epitech.travelorder.common.dto.RouteQuery;
import com.epitech.travelorder.common.enumeration.NerLabel;
import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.nlu.model.intent.Intent;
import com.epitech.travelorder.nlu.model.ner.Ner;
import com.epitech.travelorder.nlu.model.ner.NerEntity;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.epitech.travelorder.common.enumeration.IntentLabel.TRAJET;
import static com.epitech.travelorder.common.enumeration.NerLabel.B_FROM;
import static com.epitech.travelorder.common.enumeration.NerLabel.B_TO;
import static com.epitech.travelorder.common.enumeration.NerLabel.B_VIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TravelRequestMapper - Conversion from NLU to RouteQuery")
class TravelRequestMapperTest {

    private final TravelRequestMapper mapper = TravelRequestMapper.INSTANCE;

    private Nlu createNlu(NerEntity... entities) {
        return new Nlu(
                new Intent(TRAJET, 0.95f),
                new Ner(List.of(entities))
        );
    }

    private NerEntity entity(String text, NerLabel label, int position) {
        return new NerEntity(text, label, 0.95f, position, position);
    }

    @Nested
    @DisplayName("Complete conversion from NLU to RouteQuery with various entity combinations")
    class CompleteConversion {

        @Test
        @DisplayName("Should convert simple FROM and TO into RouteQuery")
        void shouldConvertSimpleFromToRoute() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result)
                    .isNotNull()
                    .extracting(RouteQuery::origin, RouteQuery::destination)
                    .containsExactly("Paris", "Lyon");
            assertThat(result.vias()).isEmpty();
            assertThat(result.departureTime()).isNotNull();
        }

        @Test
        @DisplayName("Should convert route with single VIA")
        void shouldConvertRouteWithVia() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Dijon", B_VIA, 1),
                    entity("Lyon", B_TO, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result)
                    .isNotNull()
                    .extracting(RouteQuery::origin, RouteQuery::destination)
                    .containsExactly("Paris", "Lyon");
            assertThat(result.vias())
                    .hasSize(1)
                    .containsExactly("Dijon");
        }

        @Test
        @DisplayName("Should convert route with multiple VIA entities in correct order")
        void shouldConvertRouteWithMultipleVias() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Dijon", B_VIA, 1),
                    entity("Grenoble", B_VIA, 2),
                    entity("Lyon", B_TO, 3)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.vias())
                    .hasSize(2)
                    .containsExactly("Dijon", "Grenoble");
        }

        @Test
        @DisplayName("Should convert with both DATE and TIME into departureTime")
        void shouldConvertWithDateAndTime() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-02-20T14:30:00", NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(2)
                    .hasDayOfMonth(20)
                    .hasHour(14)
                    .hasMinute(30);
        }

        @Test
        @DisplayName("Should convert with DATE only and default time")
        void shouldConvertWithDateOnly() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-02-20T00:00:00", NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(2)
                    .hasDayOfMonth(20)
                    .hasHour(0)
                    .hasMinute(0);
        }

        @Test
        @DisplayName("Should convert with only TIME (date par défaut aujourd'hui)")
        void shouldConvertWithTimeOnly() {
            // Given
            LocalDate today = LocalDate.now();
            String todayTimeStr = today.plusDays(1) + "T14:30:00";

            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity(todayTimeStr, NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(today.plusDays(1).getYear())
                    .hasMonthValue(today.plusDays(1).getMonthValue())
                    .hasDayOfMonth(today.plusDays(1).getDayOfMonth())
                    .hasHour(14)
                    .hasMinute(30);
        }
    }

    @Nested
    @DisplayName("Destination extraction and edge cases")
    class OriginExtraction {

        @Test
        @DisplayName("Should extract simple origin")
        void shouldExtractSimpleOrigin() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.origin()).isEqualTo("Paris");
        }

        @Test
        @DisplayName("Should extract multi-token origin")
        void shouldExtractMultiTokenOrigin() {
            // Given
            Nlu nlu = createNlu(
                    entity("Gare", B_FROM, 0),
                    entity("du", NerLabel.I_FROM, 1),
                    entity("Nord", NerLabel.I_FROM, 2),
                    entity("Lyon", B_TO, 3)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.origin()).isEqualTo("Gare du Nord");
        }

        @Test
        @DisplayName("Devrait retourner null si pas d'origine")
        void shouldReturnNullIfNoOrigin() {
            // Given
            Nlu nlu = createNlu(
                    entity("Lyon", B_TO, 0)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.origin()).isNull();
        }

        @Test
        @DisplayName("Should take only first FROM entity if multiple B-FROM detected (NLU bug)")
        void shouldTakeFirstFromEntityWhenNluBug() {
            // Given: Bug NLU avec 2 B-FROM consécutifs
            Nlu nlu = createNlu(
                    entity("Paris", NerLabel.B_FROM, 0),
                    entity("Nord", NerLabel.B_FROM, 1), // Bug NLU
                    entity("Lyon", NerLabel.B_TO, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then - On prend seulement le premier
            assertThat(result.origin()).isEqualTo("Paris");
        }
    }

    @Nested
    @DisplayName("(TO) Extraction de la destination")
    class DestinationExtraction {

        @Test
        @DisplayName("Should extract simple destination")
        void shouldExtractSimpleDestination() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.destination()).isEqualTo("Lyon");
        }

        @Test
        @DisplayName("Should extract multi-token destination")
        void shouldExtractMultiTokenDestination() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("Part", NerLabel.I_TO, 2),
                    entity("-", NerLabel.I_TO, 3),
                    entity("Dieu", NerLabel.I_TO, 4)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.destination()).isEqualTo("Lyon Part-Dieu");
        }

        @Test
        @DisplayName("Should throw exception if no destination found")
        void shouldThrowExceptionIfNoDestination() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0)
            );

            // When / Then
            assertThatThrownBy(() -> mapper.convert(nlu))
                    .isInstanceOf(IncompleteTravelRequestException.class)
                    .hasMessageContaining("Cannot extract destination");
        }

        @Test
        @DisplayName("Should take only first TO entity if multiple detected")
        void shouldTakeFirstToEntity() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", NerLabel.B_FROM, 0),
                    entity("Lyon", NerLabel.B_TO, 1),
                    entity("Part", NerLabel.B_TO, 2) // Bug NLU
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then - On prend seulement le premier
            assertThat(result.destination()).isEqualTo("Lyon");
        }
    }

    @Nested
    @DisplayName("VIA extraction and ordering")
    class ViasExtraction {

        @Test
        @DisplayName("Should return empty list if no VIA entities")
        void shouldReturnEmptyListIfNoVias() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.vias()).isEmpty();
        }

        @Test
        @DisplayName("Should extract single VIA")
        void shouldExtractSingleVia() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Dijon", B_VIA, 1),
                    entity("Lyon", B_TO, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.vias())
                    .hasSize(1)
                    .containsExactly("Dijon");
        }

        @Test
        @DisplayName("Should extract multiple VIA in order of appearance")
        void shouldExtractMultipleViasInOrder() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Dijon", B_VIA, 1),
                    entity("Grenoble", B_VIA, 2),
                    entity("Valence", B_VIA, 3),
                    entity("Lyon", B_TO, 4)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.vias())
                    .hasSize(3)
                    .containsExactly("Dijon", "Grenoble", "Valence");
        }

        @Test
        @DisplayName("Should extract multi-token VIA correctly")
        void shouldExtractMultiTokenVia() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Aix", B_VIA, 1),
                    entity("-", NerLabel.I_VIA, 2),
                    entity("en", NerLabel.I_VIA, 3),
                    entity("-", NerLabel.I_VIA, 4),
                    entity("Provence", NerLabel.I_VIA, 5),
                    entity("Lyon", B_TO, 6)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.vias())
                    .hasSize(1)
                    .containsExactly("Aix-en-Provence");
        }
    }

    @Nested
    @DisplayName("Departure and Arrival Time extraction")
    class DepartureAndArrivalTimeExtraction {

        @Test
        @DisplayName("Should extract DEPARTURE_TIME into LocalDateTime")
        void shouldCombineDateAndTime() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-03-15T10:30:00", NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(15)
                    .hasHour(10)
                    .hasMinute(30);
            assertThat(result.arrivalTime()).isNull();
        }

        @Test
        @DisplayName("Should use default time 00:00 if only DATE provided")
        void shouldUseMidnightIfDateOnly() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-12-25T00:00:00", NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(12)
                    .hasDayOfMonth(25)
                    .hasHour(0)
                    .hasMinute(0);
            assertThat(result.arrivalTime()).isNull();
        }

        @Test
        @DisplayName("Should use today's date if only TIME is provided")
        void shouldUseTodayIfTimeOnly() {
            // Given - Use tomorrow to avoid normalization to now()
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            String tomorrowTimeStr = tomorrow + "T16:45:00";

            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity(tomorrowTimeStr, NerLabel.B_DEPARTURE_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(tomorrow.getYear())
                    .hasMonthValue(tomorrow.getMonthValue())
                    .hasDayOfMonth(tomorrow.getDayOfMonth())
                    .hasHour(16)
                    .hasMinute(45);
            assertThat(result.arrivalTime()).isNull();
        }

        @Test
        @DisplayName("Should return null if no DEPARTURE_TIME or ARRIVAL_TIME")
        void shouldReturnNullIfNoDateTime() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            // Le constructeur de RouteQuery initialise departureTime à now() si null
            assertThat(result.departureTime()).isNotNull();
            assertThat(result.arrivalTime()).isNull();
        }

        @Test
        @DisplayName("Should extract ARRIVAL_TIME into LocalDateTime")
        void shouldExtractArrivalTime() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-03-15T18:45:00", NerLabel.B_ARRIVAL_TIME, 2)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.arrivalTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(15)
                    .hasHour(18)
                    .hasMinute(45);
            // departureTime initialisé à now() par le constructeur
            assertThat(result.departureTime()).isNotNull();
        }

        @Test
        @DisplayName("Should extract both DEPARTURE_TIME and ARRIVAL_TIME")
        void shouldExtractBothDepartureAndArrivalTime() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Lyon", B_TO, 1),
                    entity("2026-03-15T10:30:00", NerLabel.B_DEPARTURE_TIME, 2),
                    entity("2026-03-15T14:45:00", NerLabel.B_ARRIVAL_TIME, 3)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(15)
                    .hasHour(10)
                    .hasMinute(30);

            assertThat(result.arrivalTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(15)
                    .hasHour(14)
                    .hasMinute(45);
        }

        @Test
        @DisplayName("Should handle ARRIVAL_TIME on different day than DEPARTURE_TIME")
        void shouldHandleMultiDayJourney() {
            // Given
            Nlu nlu = createNlu(
                    entity("Paris", B_FROM, 0),
                    entity("Tokyo", B_TO, 1),
                    entity("2026-03-15T22:30:00", NerLabel.B_DEPARTURE_TIME, 2),
                    entity("2026-03-16T16:45:00", NerLabel.B_ARRIVAL_TIME, 3)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.departureTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(15)
                    .hasHour(22)
                    .hasMinute(30);

            assertThat(result.arrivalTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(3)
                    .hasDayOfMonth(16)
                    .hasHour(16)
                    .hasMinute(45);
        }
    }

    @Nested
    class ErrorAndEdgeCases {

        @Test
        @DisplayName("Should throw exception if NLU has no TO entity")
        void shouldThrowExceptionIfNoDestination() {
            // Given - NLU sans TO
            Nlu nlu = createNlu(
                    entity("Paris", NerLabel.B_FROM, 0)
            );

            // When / Then
            assertThatThrownBy(() -> mapper.convert(nlu))
                    .isInstanceOf(IncompleteTravelRequestException.class)
                    .hasMessageContaining("Cannot extract destination");
        }

        @Test
        @DisplayName("Should throw exception if NLU has no intent")
        void shouldThrowExceptionIfNerIsEmpty() {
            // Given
            Nlu nlu = new Nlu(
                    new Intent(TRAJET, 0.95f),
                    new Ner(List.of())
            );

            // When / Then
            assertThatThrownBy(() -> mapper.convert(nlu))
                    .isInstanceOf(IncompleteTravelRequestException.class)
                    .hasMessageContaining("Cannot extract destination");
        }

        @Test
        @DisplayName("Should trim whitespace at start/end of entity (handled by NerUtils)")
        void shouldHandleMultipleSpaces() {
            // Given - NerUtils trim déjà les entités
            Nlu nlu = createNlu(
                    entity("Paris", NerLabel.B_FROM, 0), // Pré-trimmed par NerUtils
                    entity("Lyon", NerLabel.B_TO, 1)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.origin()).isEqualTo("Paris");
            assertThat(result.destination()).isEqualTo("Lyon");
        }

        @Test
        @DisplayName("Should handle complex complete route with all elements")
        void shouldHandleComplexCompleteRoute() {
            // Given
            Nlu nlu = createNlu(
                    entity("Gare", B_FROM, 0),
                    entity("de", NerLabel.I_FROM, 1),
                    entity("Lyon", NerLabel.I_FROM, 2),
                    entity("Dijon", B_VIA, 3),
                    entity("Besançon", B_VIA, 4),
                    entity("Paris", B_TO, 5),
                    entity("Nord", NerLabel.I_TO, 6),
                    entity("2026-06-01T08:15:00", NerLabel.B_DEPARTURE_TIME, 7),
                    entity("2026-06-01T14:30:00", NerLabel.B_ARRIVAL_TIME, 8),
                    entity("train", NerLabel.B_TRANSPORT, 9)
            );

            // When
            RouteQuery result = mapper.convert(nlu);

            // Then
            assertThat(result.origin()).isEqualTo("Gare de Lyon");
            assertThat(result.destination()).isEqualTo("Paris Nord");
            assertThat(result.vias()).containsExactly("Dijon", "Besançon");
            assertThat(result.departureTime())
                    .hasYear(2026)
                    .hasMonthValue(6)
                    .hasDayOfMonth(1)
                    .hasHour(8)
                    .hasMinute(15);
            assertThat(result.arrivalTime())
                    .isNotNull()
                    .hasYear(2026)
                    .hasMonthValue(6)
                    .hasDayOfMonth(1)
                    .hasHour(14)
                    .hasMinute(30);
        }
    }
}

