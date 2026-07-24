package br.com.artheus.kairos.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, String> {

    Optional<Plan> findByUserId(String userId);

    List<Plan> findByExpiresAtBefore(LocalDateTime dateTime);

    List<Plan> findByExpiresAtBeforeAndPlanTypeNot(LocalDateTime dateTime, PlanType planType);

    List<Plan> findByExpiresAtBeforeAndPlanType(LocalDateTime dateTime, PlanType planType);

    Optional<Plan> findByStripeCustomerId(String stripeCustomerId);

    boolean existsByUserId(String userId);
}
