package br.com.artheus.kairos.plan;

import jakarta.validation.constraints.NotNull;

public record PlanRequest(

        @NotNull(message = "Plan Type cannot be null")
        PlanType planType
) {
}
