package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceDataProvider;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.risk.RiskProvider;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngine {

    private final CityProvider cityProvider;
    private final ClimateDataProvider climateDataProvider;
    private final OccurrenceDataProvider occurrenceDataProvider;
    private final RiskCalculator riskCalculator;
    private final RiskProvider riskProvider;

    public void calculateRiskForAllCities() {
        log.info("[RISK DETECT] Starting risk calculation for all active cities...");
        List<CityLocation> activeCities = cityProvider.findActiveCities();

        for (CityLocation city : activeCities) {
            try {
                log.info("[RISK DETECT] Fetching latest climate data for city: {}", city.name());
                ClimateDataSummary climate = climateDataProvider.findLatestByCity(city.id());

                log.info("[RISK DETECT] Fetching verified occurrences for city: {}", city.name());
                List<OccurrenceSummary> occurrences = occurrenceDataProvider.findVerifiedByCityId(city.id());

                log.info("[RISK DETECT] Calculating current risk level...");
                RiskLevel riskLevel = riskCalculator.calculate(climate, occurrences);

                log.info("[RISK DETECT] Saving calculated risk ({}) for city ID: {}", riskLevel, city.id());
                riskProvider.saveCalculatedRisk(city.id(), riskLevel);

            } catch (Exception e) {
                // Using '{}' placeholders in log.error to avoid manual String concatenation
                log.error("[RISK DETECT] CRITICAL ERROR calculating risk for city: {} (ID: {})", city.name(), city.id(), e);
            }
        }
        log.info("[RISK DETECT] RiskEngine processing finished.");
    }
}