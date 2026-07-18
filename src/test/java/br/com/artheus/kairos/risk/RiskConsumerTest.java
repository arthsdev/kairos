package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskConsumer Unit Tests")
class RiskConsumerTest {

    @Mock
    private RiskService riskService;

    @InjectMocks
    private RiskConsumer riskConsumer;

    @Nested
    @DisplayName("Tests for fetchCalculatedRisk")
    class FetchCalculatedRiskTests {

        private final RiskMessage mockMessage = new RiskMessage(
                "city-123", 25.0, 75, 10.0, 12.5
        );

        @Test
        @DisplayName("Should process risk calculation successfully without throwing exceptions")
        void shouldProcessSuccessfully() {
            // Given
            doNothing().when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                    riskConsumer.fetchCalculatedRisk(mockMessage)
            );

            verify(riskService, times(1)).processRiskCalculation(mockMessage);
        }

        @Test
        @DisplayName("Should throw AmqpRejectAndDontRequeueException when BusinessException occurs")
        void shouldRejectAndSendToDlqOnBusinessException() {
            // Given
            BusinessException businessException = new BusinessException("Invalid climate metrics combination.");
            doThrow(businessException).when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            assertThatThrownBy(() -> riskConsumer.fetchCalculatedRisk(mockMessage))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in risk processing.")
                    .hasCause(businessException);
        }

        @Test
        @DisplayName("Should throw AmqpRejectAndDontRequeueException when ResourceNotFoundException occurs")
        void shouldRejectAndSendToDlqOnResourceNotFoundException() {
            // Given
            ResourceNotFoundException notFoundException = new ResourceNotFoundException("City metadata not found.");
            doThrow(notFoundException).when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            assertThatThrownBy(() -> riskConsumer.fetchCalculatedRisk(mockMessage))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in risk processing.")
                    .hasCause(notFoundException);
        }

        @Test
        @DisplayName("Should throw AmqpRejectAndDontRequeueException when ExternalServiceException occurs")
        void shouldRejectAndSendToDlqOnExternalServiceException() {
            ExternalServiceException externalException = new ExternalServiceException(
                    "EXTERNAL_SERVICE_ERROR",
                    "Provider API timeout.",
                    new RuntimeException("Timeout")
            );
            doThrow(externalException).when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            assertThatThrownBy(() -> riskConsumer.fetchCalculatedRisk(mockMessage))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in risk processing.")
                    .hasCause(externalException);
        }

        @Test
        @DisplayName("Should throw AmqpRejectAndDontRequeueException when DataIntegrityViolationException occurs")
        void shouldRejectAndSendToDlqOnDataIntegrityViolation() {
            // Given
            DataIntegrityViolationException integrityException = new DataIntegrityViolationException("Duplicate key or foreign key constraint failed.");
            doThrow(integrityException).when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            assertThatThrownBy(() -> riskConsumer.fetchCalculatedRisk(mockMessage))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Data integrity violation in risk processing.")
                    .hasCause(integrityException);
        }

        @Test
        @DisplayName("Should rethrow the exact same exception when an unmapped unexpected error occurs")
        void shouldRethrowGenericException() {
            // Given
            RuntimeException unexpectedException = new RuntimeException("Unexpected infrastructure breakdown.");
            doThrow(unexpectedException).when(riskService).processRiskCalculation(mockMessage);

            // When & Then
            assertThatThrownBy(() -> riskConsumer.fetchCalculatedRisk(mockMessage))
                    .isSameAs(unexpectedException);
        }
    }
}