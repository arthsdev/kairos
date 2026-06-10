package br.com.artheus.kairos.shared.scheduler;

import br.com.artheus.kairos.climate.ClimateService;
import br.com.artheus.kairos.plan.PlanService;
import br.com.artheus.kairos.risk.RiskEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KairosScheduler {

    private final PlanService planService;
    private final ClimateService climateService;
    private final RiskEngine riskEngine;

    @Scheduled(fixedRateString = "${config.scheduler.plans-interval}")
    public void checkExpiredPlans() {
        log.info("Starting expired plans check...");

        planService.checkExpiredPlans();

        log.info("Expired plans check finished.");
    }

    @Scheduled(fixedRateString = "${config.scheduler.climate-interval}")
    public void fetchClimateData() {
        log.info("Starting climate data fetch and risk calculation...");
        climateService.fetchClimateDataForAllCities();
        riskEngine.calculateRiskForAllCities();
        log.info("Climate data fetch and risk calculation finished.");
    }
}
