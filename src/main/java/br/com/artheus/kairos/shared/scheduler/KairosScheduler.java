package br.com.artheus.kairos.shared.scheduler;

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

    @Scheduled(fixedRateString = "${config.scheduler.plans-interval}")
    public void checkExpiredPlans() {
        log.info("Starting expired plans check...");

        planService.checkExpiredPlans();

        log.info("Expired plans check finished.");
    }
}
