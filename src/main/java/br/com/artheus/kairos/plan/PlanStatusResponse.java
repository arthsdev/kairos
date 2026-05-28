package br.com.artheus.kairos.plan;

import java.time.LocalDateTime;

/**
 * DTO used to represent a user current plan status,
 * containing the plan type and expiration date.
 */
public record PlanStatusResponse(PlanType planType,
                                 LocalDateTime expiresAt) {

    public static PlanStatusResponse from(Plan plan) {
        return new PlanStatusResponse(plan.getPlanType(), plan.getExpiresAt());
    }
}