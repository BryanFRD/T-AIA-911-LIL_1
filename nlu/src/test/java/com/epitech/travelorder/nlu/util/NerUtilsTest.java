package com.epitech.travelorder.nlu.util;

import com.epitech.travelorder.common.enumeration.NerLabel;
import com.epitech.travelorder.nlu.model.ner.NerEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NerUtils - NER Entity Grouping Tests")
class NerUtilsTest {

    @Nested
    @DisplayName("Simple cases - single entities")
    class SimpleEntities {

        @Test
        @DisplayName("Should extract simple FROM entity")
        void shouldExtractSimpleFromEntity() {
            // Given
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Paris"));
        }

        @Test
        @DisplayName("Should extract FROM and TO separately")
        void shouldExtractFromAndToEntities() {
            // Given
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Lyon", NerLabel.B_TO, 1)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(2)
                .containsEntry("FROM", List.of("Paris"))
                .containsEntry("TO", List.of("Lyon"));
        }

        @Test
        @DisplayName("Should return empty map if no entities")
        void shouldReturnEmptyMapWhenNoEntities() {
            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty map if only O (Outside) tokens")
        void shouldReturnEmptyMapWhenOnlyOutsideTokens() {
            // Given
            List<NerEntity> entities = List.of(
                entity("Je", NerLabel.O, 0),
                entity("veux", NerLabel.O, 1),
                entity("voyager", NerLabel.O, 2)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("BIO Reconstruction - B- and I- tags")
    class BioTagReconstruction {

        @Test
        @DisplayName("Should reconstruct multi-token entity with hyphens (B-FROM + I-FROM)")
        void shouldReconstructHyphenatedEntity() {
            // Given: "Saint-Amant-les-Eaux"
            List<NerEntity> entities = List.of(
                entity("Saint", NerLabel.B_FROM, 0),
                entity("-", NerLabel.I_FROM, 1),
                entity("Amant", NerLabel.I_FROM, 2),
                entity("-", NerLabel.I_FROM, 3),
                entity("les", NerLabel.I_FROM, 4),
                entity("-", NerLabel.I_FROM, 5),
                entity("Eaux", NerLabel.I_FROM, 6)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Saint-Amant-les-Eaux"));
        }

        @Test
        @DisplayName("Should reconstruct multi-word entity with spaces")
        void shouldReconstructMultiWordEntity() {
            // Given: "Gare de Lyon Part-Dieu"
            List<NerEntity> entities = List.of(
                entity("Gare", NerLabel.B_FROM, 0),
                entity("de", NerLabel.I_FROM, 1),
                entity("Lyon", NerLabel.I_FROM, 2),
                entity("Part", NerLabel.I_FROM, 3),
                entity("-", NerLabel.I_FROM, 4),
                entity("Dieu", NerLabel.I_FROM, 5)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Gare de Lyon Part-Dieu"));
        }

        @Test
        @DisplayName("Should handle apostrophes without space")
        void shouldHandleApostrophes() {
            // Given: "L'Hôpital"
            List<NerEntity> entities = List.of(
                entity("L", NerLabel.B_FROM, 0),
                entity("'", NerLabel.I_FROM, 1),
                entity("Hôpital", NerLabel.I_FROM, 2)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("L'Hôpital"));
        }
    }

    @Nested
    @DisplayName("NLU bugs handling - multiple consecutive B- tags")
    class MultipleBTagsHandling {

        @Test
        @DisplayName("Should create two separate FROM entities if two consecutive B-FROM tags (NLU bug)")
        void shouldHandleTwoConsecutiveBFromTags() {
            // Given: Bug NLU qui envoie 2 B-FROM au lieu de B-FROM + I-FROM
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Nord", NerLabel.B_FROM, 1) // Devrait être I-FROM mais le NLU a un bug
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then - On doit avoir 2 entités FROM distinctes
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Paris", "Nord"));
        }

        @Test
        @DisplayName("Should handle B-FROM + I-FROM then new B-FROM correctly")
        void shouldHandleNewBTagAfterITags() {
            // Given: "Saint-Denis" puis "Gare du Nord" (2 entités FROM distinctes)
            List<NerEntity> entities = List.of(
                entity("Saint", NerLabel.B_FROM, 0),
                entity("-", NerLabel.I_FROM, 1),
                entity("Denis", NerLabel.I_FROM, 2),
                entity("Gare", NerLabel.B_FROM, 3), // Nouveau B-FROM = nouvelle entité
                entity("du", NerLabel.I_FROM, 4),
                entity("Nord", NerLabel.I_FROM, 5)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Saint-Denis", "Gare du Nord"));
        }

        @Test
        @DisplayName("Should treat orphan I-FROM (without preceding B-FROM) as new entity")
        void shouldHandleOrphanITag() {
            // Given: I-FROM sans B-FROM avant (après un token O)
            List<NerEntity> entities = List.of(
                entity("vers", NerLabel.O, 0),
                entity("Paris", NerLabel.I_FROM, 1) // I-FROM orphelin
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then - L'I-FROM orphelin est traité comme une nouvelle entité
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Paris"));
        }
    }

    @Nested
    @DisplayName("Mixed and complex entities")
    class MixedAndComplexEntities {

        @Test
        @DisplayName("Should extract FROM, TO, VIA in complete sentence")
        void shouldExtractMultipleEntityTypes() {
            // Given: "De Paris via Dijon vers Lyon"
            List<NerEntity> entities = List.of(
                entity("De", NerLabel.O, 0),
                entity("Paris", NerLabel.B_FROM, 1),
                entity("via", NerLabel.O, 2),
                entity("Dijon", NerLabel.B_VIA, 3),
                entity("vers", NerLabel.O, 4),
                entity("Lyon", NerLabel.B_TO, 5)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(3)
                .containsEntry("FROM", List.of("Paris"))
                .containsEntry("VIA", List.of("Dijon"))
                .containsEntry("TO", List.of("Lyon"));
        }

        @Test
        @DisplayName("Should handle DEPARTURE_TIME and ARRIVAL_TIME separately")
        void shouldHandleDateAndTime() {
            // Given: "départ demain à 10h30, arrivée à 14h45"
            List<NerEntity> entities = List.of(
                entity("2026-02-14T10:30:00", NerLabel.B_DEPARTURE_TIME, 0),
                entity("2026-02-14T14:45:00", NerLabel.B_ARRIVAL_TIME, 1)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(2)
                .containsEntry("DEPARTURE_TIME", List.of("2026-02-14T10:30:00"))
                .containsEntry("ARRIVAL_TIME", List.of("2026-02-14T14:45:00"));
        }

        @Test
        @DisplayName("Should extract all supported entity types")
        void shouldExtractAllSupportedEntityTypes() {
            // Given: Phrase complète avec tous les types
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Lyon", NerLabel.B_TO, 1),
                entity("train", NerLabel.B_TRANSPORT, 2),
                entity("2026-02-15T10:00:00", NerLabel.B_DEPARTURE_TIME, 3)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(4)
                .containsEntry("FROM", List.of("Paris"))
                .containsEntry("TO", List.of("Lyon"))
                .containsEntry("TRANSPORT", List.of("train"))
                .containsEntry("DEPARTURE_TIME", List.of("2026-02-15T10:00:00"));
        }

        @Test
        @DisplayName("Should handle multiple VIA")
        void shouldHandleMultipleVias() {
            // Given: "Paris via Dijon via Grenoble vers Lyon"
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Dijon", NerLabel.B_VIA, 1),
                entity("Grenoble", NerLabel.B_VIA, 2),
                entity("Lyon", NerLabel.B_TO, 3)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(3)
                .containsEntry("FROM", List.of("Paris"))
                .containsEntry("VIA", List.of("Dijon", "Grenoble"))
                .containsEntry("TO", List.of("Lyon"));
        }
    }

    @Nested
    @DisplayName("Entity type changes")
    class EntityTypeChanges {

        @Test
        @DisplayName("Should start new entity if type changes (FROM -> TO)")
        void shouldStartNewEntityOnTypeChange() {
            // Given: I-TO après B-FROM (changement de type)
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Lyon", NerLabel.I_TO, 1) // Changement de type FROM -> TO
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(2)
                .containsEntry("FROM", List.of("Paris"))
                .containsEntry("TO", List.of("Lyon"));
        }

        @Test
        @DisplayName("Should handle O token in the middle of entities")
        void shouldHandleOutsideTokenBetweenEntities() {
            // Given
            List<NerEntity> entities = List.of(
                entity("Paris", NerLabel.B_FROM, 0),
                entity("Nord", NerLabel.I_FROM, 1),
                entity("à", NerLabel.O, 2),
                entity("Lyon", NerLabel.B_TO, 3),
                entity("Part", NerLabel.I_TO, 4),
                entity("-", NerLabel.I_TO, 5),
                entity("Dieu", NerLabel.I_TO, 6)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .hasSize(2)
                .containsEntry("FROM", List.of("Paris Nord"))
                .containsEntry("TO", List.of("Lyon Part-Dieu"));
        }
    }

    @Nested
    @DisplayName("extractDateTime - Date parsing")
    class ExtractDateTime {

        @Test
        @DisplayName("Should parse valid ISO date")
        void shouldParseValidIsoDateTime() {
            // Given
            String dateTimeStr = "2026-02-15T14:30:00";

            // When
            LocalDateTime result = NerUtils.extractDateTime(dateTimeStr);

            // Then
            assertThat(result)
                .isNotNull()
                .hasYear(2026)
                .hasMonthValue(2)
                .hasDayOfMonth(15)
                .hasHour(14)
                .hasMinute(30)
                .hasSecond(0);
        }

        @Test
        @DisplayName("Should return now if format is invalid")
        void shouldReturnNowForInvalidFormat() {
            // Given
            String invalidDateTime = "invalid-date";
            LocalDateTime before = LocalDateTime.now();

            // When
            LocalDateTime result = NerUtils.extractDateTime(invalidDateTime);

            // Then
            assertThat(result)
                .isNotNull()
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should handle date with only date part")
        void shouldHandleDateOnly() {
            // Given
            String dateTimeStr = "2026-03-20T00:00:00";

            // When
            LocalDateTime result = NerUtils.extractDateTime(dateTimeStr);

            // Then
            assertThat(result)
                .isNotNull()
                .hasYear(2026)
                .hasMonthValue(3)
                .hasDayOfMonth(20)
                .hasHour(0)
                .hasMinute(0);
        }
    }

    @Nested
    @DisplayName("Edge cases and limits")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null list gracefully")
        void shouldHandleNullListGracefully() {
            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should trim whitespace at start/end of entity")
        void shouldTrimWhitespace() {
            // Given
            List<NerEntity> entities = List.of(
                entity("  Paris  ", NerLabel.B_FROM, 0)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsEntry("FROM", List.of("Paris"));
        }

        @Test
        @DisplayName("Should handle very long sequence of I- tags")
        void shouldHandleLongSequenceOfITags() {
            // Given: Une très longue adresse
            List<NerEntity> entities = List.of(
                entity("Gare", NerLabel.B_FROM, 0),
                entity("de", NerLabel.I_FROM, 1),
                entity("Lyon", NerLabel.I_FROM, 2),
                entity("Part", NerLabel.I_FROM, 3),
                entity("-", NerLabel.I_FROM, 4),
                entity("Dieu", NerLabel.I_FROM, 5),
                entity("Avenue", NerLabel.I_FROM, 6),
                entity("Thiers", NerLabel.I_FROM, 7)
            );

            // When
            Map<String, List<String>> result = NerUtils.groupEntitiesByType(entities);

            // Then
            assertThat(result)
                .containsOnlyKeys("FROM")
                .containsEntry("FROM", List.of("Gare de Lyon Part-Dieu Avenue Thiers"));
        }
    }

    private NerEntity entity(String text, NerLabel label, int position) {
        return new NerEntity(text, label, 0.95f, position, position);
    }
}

