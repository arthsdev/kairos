package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateInput;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceDataProvider;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.contract.risk.RiskProvider;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskConsumer {

    private final RiskCalculator riskCalculator;
    private final RiskProvider riskProvider;
    private final OccurrenceDataProvider occurrenceDataProvider;

    @RabbitListener(queues = RabbitMQConfig.CALCULATE_RISK)
    public void fetchCalculatedRisk(RiskMessage riskMessage) {

        List<OccurrenceSummary> occurrences = occurrenceDataProvider.findVerifiedByCityId(riskMessage.cityId());

        ClimateInput climate = new ClimateInput(
                riskMessage.temperature(),
                riskMessage.humidity(),
                riskMessage.rainVolume(),
                riskMessage.windSpeed()

        );

        RiskLevel calculatedRisk = riskCalculator.calculate(climate, occurrences);

        riskProvider.saveCalculatedRisk(riskMessage.cityId(), calculatedRisk);

        log.info("Risk calculated for city: {}", riskMessage.cityId());

    }
}
