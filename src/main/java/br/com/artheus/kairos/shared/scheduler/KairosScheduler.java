package br.com.artheus.kairos.shared.scheduler;

import br.com.artheus.kairos.climate.ClimateService;
import br.com.artheus.kairos.plan.PlanService;
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

    @Scheduled(fixedRateString = "${config.scheduler.plans-interval}")
    public void checkExpiredPlans() {
        log.info("Starting expired plans check...");

        planService.checkExpiredPlans();

        log.info("Expired plans check finished.");
    }

    @Scheduled(fixedRateString = "${config.scheduler.climate-interval}")
    public void fetchClimateData() {
        log.info("Starting climate data fetch...");
        climateService.fetchClimateDataForAllCities();
        log.info("Climate data fetch finished.");
    }
}
