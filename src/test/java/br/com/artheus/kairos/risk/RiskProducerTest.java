package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RiskProducer riskProducer;

    @Test
    @DisplayName("Should publish RiskMessage to the correct exchange and routing key")
    void shouldPublishMessageCorrectly() {
        RiskMessage message = new RiskMessage("city-123", 25.0, 70, 10.0, 15.0);

        riskProducer.publish(message);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.RISK_EXCHANGE,
                RabbitMQConfig.CALCULATE_RISK,
                message
        );
    }
}