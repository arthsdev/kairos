package br.com.artheus.kairos.shared.scheduler;

import br.com.artheus.kairos.climate.ClimateProducer;
import br.com.artheus.kairos.plan.PlanService;
import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KairosScheduler {

    private final PlanService planService;
    private final CityProvider cityProvider;
    private final ClimateProducer climateProducer;

    @Scheduled(fixedRateString = "${config.scheduler.plans-interval}")
    public void checkExpiredPlans() {
        log.info("Starting expired plans check...");

        planService.checkExpiredPlans();

        log.info("Expired plans check finished.");
    }

    @Scheduled(fixedRateString = "${config.scheduler.climate-interval}")
    public void fetchClimateData() {

        log.info("fetchClimateData triggered");

        List<CityLocation> activeCities = cityProvider.findActiveCities();

        for (CityLocation cityLocation : activeCities) {

            climateProducer.publishClimateData(new ClimateFetchRequest(cityLocation.id(), cityLocation.latitude(), cityLocation.longitude()));

            log.info("Climate data fetch requested for city ID: {}", cityLocation.id());
        }
    }

}
