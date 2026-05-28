package br.com.artheus.kairos.plan;

import java.time.LocalDateTime;

public record PlanResponse(

        String id,

        String userId,

        PlanType planType,

        int cityLimit,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        LocalDateTime expiresAt
) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getUserId(),
                plan.getPlanType(),
                plan.getCityLimit(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getExpiresAt()
        );
    }
}
