package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClimateProducer Unit Tests")
class ClimateProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ClimateProducer climateProducer;

    @Nested
    @DisplayName("Tests for publishClimateData")
    class PublishClimateDataTests {

        @Test
        @DisplayName("Should invoke rabbitTemplate with exact exchange, routing key and payload")
        void shouldPublishMessageWithCorrectRoutingAndPayload() {
            // Given
            ClimateFetchRequest request = new ClimateFetchRequest("city-99", -26.3, -48.8);

            // When & Then
            // Garantimos que a execução flui sem lançar exceções
            assertThatCode(() -> climateProducer.publishClimateData(request))
                    .doesNotThrowAnyException();

            // Verificação crucial: garante que o contrato de roteamento do RabbitMQ não foi quebrado
            verify(rabbitTemplate, times(1)).convertAndSend(
                    RabbitMQConfig.CLIMATE_EXCHANGE,
                    RabbitMQConfig.FETCH_CLIMATE_DATA,
                    request
            );
        }
    }
}