package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskConsumer {

    private final RiskService riskService;

    @RabbitListener(queues = RabbitMQConfig.CALCULATE_RISK)
    public void fetchCalculatedRisk(RiskMessage riskMessage) {

        riskService.processRiskCalculation(riskMessage);
    }
}