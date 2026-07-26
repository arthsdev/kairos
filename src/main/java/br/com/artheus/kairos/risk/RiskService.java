package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateInput;
import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceDataProvider;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.contract.risk.RiskProvider;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService {

    private final OccurrenceDataProvider occurrenceDataProvider;
    private final RiskCalculator riskCalculator;
    private final RiskProvider riskProvider;
    private final CityProvider cityProvider;
    private final NotificationSender notificationSender;

    public void processRiskCalculation(RiskMessage riskMessage) {

        List<OccurrenceSummary> occurrences =
                occurrenceDataProvider.findVerifiedByCityId(riskMessage.cityId());

        ClimateInput climate = new ClimateInput(
                riskMessage.temperature(),
                riskMessage.humidity(),
                riskMessage.rainVolume(),
                riskMessage.windSpeed()
        );

        RiskLevel calculatedRisk = riskCalculator.calculate(climate, occurrences);

        riskProvider.saveCalculatedRisk(riskMessage.cityId(), calculatedRisk);

        if (calculatedRisk == RiskLevel.CRITICAL || calculatedRisk == RiskLevel.HIGH) {
            CityLocation cityLocation = cityProvider.findCityById(riskMessage.cityId());
            String message = String.format("Alert: City %s reached a risk level of %s!", cityLocation.name(), calculatedRisk);
            notificationSender.sendNotification(message);
        }

        log.info("Risk calculated for city: {}", riskMessage.cityId());
    }
}

