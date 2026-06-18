package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.risk.RiskCalculationPublisher;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskProducer implements RiskCalculationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(RiskMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RISK_EXCHANGE,
                RabbitMQConfig.CALCULATE_RISK, // routing key
                message
        );
    }
}
