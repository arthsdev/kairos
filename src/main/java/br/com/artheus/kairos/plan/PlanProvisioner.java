package br.com.artheus.kairos.plan;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanProvisioner {

    private final PlanRepository planRepository;

    public PlanProvisioner(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPlan(String userId) {
        Plan plan = Plan.builder()
                .userId(userId)
                .build();

        planRepository.saveAndFlush(plan);
    }
}