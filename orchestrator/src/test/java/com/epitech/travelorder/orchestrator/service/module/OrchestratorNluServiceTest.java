package com.epitech.travelorder.orchestrator.service.module;

import com.epitech.travelorder.nlu.model.Nlu;
import com.epitech.travelorder.nlu.model.intent.Intent;
import com.epitech.travelorder.nlu.model.ner.Ner;
import com.epitech.travelorder.nlu.service.NluService;
import com.epitech.travelorder.orchestrator.config.exception.business.IncompleteTravelRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.epitech.travelorder.common.enumeration.IntentLabel.TRAJET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestratorNluService - NLU Request Processing and Capitalization")
class OrchestratorNluServiceTest {

    @Mock
    private NluService nluService;

    @InjectMocks
    private OrchestratorNluService orchestratorNluService;

    private Nlu createMockNlu() {
        return new Nlu(
                new Intent(TRAJET, 0.95f),
                new Ner(List.of())
        );
    }

    @Nested
    @DisplayName("Capitalization for NLU compatibility")
    class CapitalizationTests {

        @Test
        @DisplayName("Should capitalize first letter of each word from lowercase input")
        void shouldCapitalizeEachWordFromLowercase() {
            // Given
            String lowercaseRequest = "je veux aller de cambrai à douai";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            Nlu result = orchestratorNluService.getUnderstandingCustomerPath(lowercaseRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            String capturedRequest = requestCaptor.getValue();

            assertThat(capturedRequest).isEqualTo("Je Veux Aller De Cambrai À Douai");
            assertThat(result).isNotNull().isEqualTo(mockNlu);
        }

        @Test
        @DisplayName("Should capitalize cities from audio service lowercase output")
        void shouldCapitalizeCitiesFromAudioService() {
            // Given - Audio service returns lowercase
            String audioRequest = "aller de saint-amant-les-eaux à paris";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(audioRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue())
                    .isEqualTo("Aller De Saint-amant-les-eaux À Paris");
        }

        @Test
        @DisplayName("Should handle already capitalized text")
        void shouldHandleAlreadyCapitalizedText() {
            // Given
            String capitalizedRequest = "Je Veux Aller De Paris À Lyon";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(capitalizedRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue()).isEqualTo("Je Veux Aller De Paris À Lyon");
        }

        @Test
        @DisplayName("Should handle mixed case text")
        void shouldHandleMixedCaseText() {
            // Given
            String mixedRequest = "je VEUX AlLeR de CAMBRAI à douai";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(mixedRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue())
                    .isEqualTo("Je Veux Aller De Cambrai À Douai");
        }

        @Test
        @DisplayName("Should handle single character words")
        void shouldHandleSingleCharacterWords() {
            // Given
            String request = "aller à paris";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(request);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue()).isEqualTo("Aller À Paris");
        }

        @Test
        @DisplayName("Should handle multiple spaces between words")
        void shouldHandleMultipleSpaces() {
            // Given
            String request = "aller  de   paris    à     lyon";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(request);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue()).isEqualTo("Aller De Paris À Lyon");
        }
    }

    @Nested
    @DisplayName("NLU Service Integration")
    class NluServiceIntegrationTests {

        @Test
        @DisplayName("Should call NLU service with capitalized request")
        void shouldCallNluServiceWithCapitalizedRequest() {
            // Given
            String request = "aller de cambrai à douai";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand("Aller De Cambrai À Douai")).thenReturn(mockNlu);

            // When
            Nlu result = orchestratorNluService.getUnderstandingCustomerPath(request);

            // Then
            verify(nluService, times(1)).understand("Aller De Cambrai À Douai");
            assertThat(result).isEqualTo(mockNlu);
        }

        @Test
        @DisplayName("Should throw exception if NLU returns null")
        void shouldThrowExceptionIfNluReturnsNull() {
            // Given
            String request = "aller de paris à lyon";
            when(nluService.understand(anyString())).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> orchestratorNluService.getUnderstandingCustomerPath(request))
                    .isInstanceOf(IncompleteTravelRequestException.class)
                    .hasMessageContaining("NLU could not understand the customer request");
        }

        @Test
        @DisplayName("Should return NLU result when successful")
        void shouldReturnNluResultWhenSuccessful() {
            // Given
            String request = "aller de paris à lyon";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            // When
            Nlu result = orchestratorNluService.getUnderstandingCustomerPath(request);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(mockNlu);
        }
    }

    @Nested
    @DisplayName("Real-world scenarios from audio service")
    class RealWorldScenarios {

        @Test
        @DisplayName("Should handle less known cities from audio service")
        void shouldHandleLessKnownCities() {
            // Given - Audio service returns: "aller de cambrai à douai"
            String audioRequest = "aller de cambrai à douai";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(audioRequest);

            // Then - NLU receives capitalized version
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue()).isEqualTo("Aller De Cambrai À Douai");
        }

        @Test
        @DisplayName("Should handle complex city names with hyphens")
        void shouldHandleComplexCityNames() {
            // Given
            String audioRequest = "aller de aix-en-provence à lyon-part-dieu";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(audioRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue())
                    .isEqualTo("Aller De Aix-en-provence À Lyon-part-dieu");
        }

        @Test
        @DisplayName("Should handle time expressions from audio")
        void shouldHandleTimeExpressions() {
            // Given
            String audioRequest = "aller de paris à lyon demain à 10 heures";
            Nlu mockNlu = createMockNlu();
            when(nluService.understand(anyString())).thenReturn(mockNlu);

            ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

            // When
            orchestratorNluService.getUnderstandingCustomerPath(audioRequest);

            // Then
            verify(nluService).understand(requestCaptor.capture());
            assertThat(requestCaptor.getValue())
                    .isEqualTo("Aller De Paris À Lyon Demain À 10 Heures");
        }
    }
}

