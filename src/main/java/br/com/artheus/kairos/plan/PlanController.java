package br.com.artheus.kairos.plan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Endpoints to manage user subscription plans and billing tiers.")
public class PlanController {

    private final PlanService planService;


    @PostMapping("/upgrade")
    // TODO: This endpoint currently upgrades the plan without payment validation.
    // Before going to production, integrate a payment gateway (e.g., Stripe, PagSeguro)
    // and protect this endpoint with payment confirmation before updating the plan.
    @Operation(
            summary = "Upgrade account to Premium",
            description = "Changes the subscription tier of the authenticated user to the Premium plan. " +
                    "NOTE: Payment integration is not yet implemented. This endpoint is a placeholder."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription upgraded successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "402", description = "Payment required or transaction failed"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (e.g., user is already premium)")
    })
    public ResponseEntity<PlanResponse> upgradeToPremium(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(planService.upgradeToPremium(userId));
    }

    @GetMapping
    @Operation(
            summary = "Get current plan status",
            description = "Retrieves details about the authenticated user's current subscription plan, active features, and limits."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "404", description = "Subscription details not found for the user")
    })
    public ResponseEntity<PlanStatusResponse> getMyPlan(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(planService.getMyPlan(userId));
    }
}
