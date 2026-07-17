package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
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
@DisplayName("ClimateConsumer Unit Tests")
class ClimateConsumerTest {

    @Mock
    private ClimateService climateService;

    @InjectMocks
    private ClimateConsumer climateConsumer;

    @Test
    @DisplayName("Should process climate fetch successfully without throwing any exception")
    void shouldProcessClimateFetchSuccessfully() {
        ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);

        climateConsumer.fetchClimateData(request);

        verify(climateService, times(1)).processClimateFetch(request);
    }

    @Nested
    @DisplayName("Tests for Permanent Failures (Routing to DLQ via AmqpRejectAndDontRequeueException)")
    class PermanentFailuresTests {

        @Test
        @DisplayName("Should route to DLQ when service throws BusinessException")
        void shouldRouteToDlqWhenBusinessExceptionOccurs() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-invalid", 0.0, 0.0);

            doThrow(new BusinessException("Invalid climate fetch request"))
                    .when(climateService).processClimateFetch(request);

            assertThatThrownBy(() -> climateConsumer.fetchClimateData(request))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in climate processing.")
                    .hasCauseInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should route to DLQ when service throws ResourceNotFoundException")
        void shouldRouteToDlqWhenResourceNotFoundExceptionOccurs() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-ghost", 0.0, 0.0);

            doThrow(new ResourceNotFoundException("This city has no climate data"))
                    .when(climateService).processClimateFetch(request);

            assertThatThrownBy(() -> climateConsumer.fetchClimateData(request))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in climate processing.")
                    .hasCauseInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should route to DLQ when service throws ExternalServiceException (Retries Exhausted)")
        void shouldRouteToDlqWhenExternalServiceExceptionOccurs() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);

            doThrow(new ExternalServiceException("CODE", "Unable to fetch weather data", new RuntimeException()))
                    .when(climateService).processClimateFetch(request);

            assertThatThrownBy(() -> climateConsumer.fetchClimateData(request))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Permanent failure or retry exhausted in climate processing.")
                    .hasCauseInstanceOf(ExternalServiceException.class);
        }

        @Test
        @DisplayName("Should route to DLQ when service throws DataIntegrityViolationException")
        void shouldRouteToDlqWhenDataIntegrityViolationOccurs() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);

            doThrow(new DataIntegrityViolationException("Database constraint violation"))
                    .when(climateService).processClimateFetch(request);

            assertThatThrownBy(() -> climateConsumer.fetchClimateData(request))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                    .hasMessageContaining("Data integrity violation.")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Tests for Unexpected or Transient Failures")
    class TransientFailuresTests {

        @Test
        @DisplayName("Should rethrow unmapped generic Exception to trigger container requeue behavior")
        void shouldRethrowGenericExceptionAsIs() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);
            RuntimeException unexpectedException = new RuntimeException("Simulated connection timeout or out of memory");

            doThrow(unexpectedException)
                    .when(climateService).processClimateFetch(request);

            assertThatThrownBy(() -> climateConsumer.fetchClimateData(request))
                    .isSameAs(unexpectedException);
        }
    }
}