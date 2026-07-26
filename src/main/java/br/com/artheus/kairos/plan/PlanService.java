package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.contract.payment.PaymentCheckoutProvider;
import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService implements PaymentWebhookProcessor {


    private final PlanRepository planRepository;
    private final PaymentCheckoutProvider paymentCheckoutProvider;
    private final BillingNotificationService billingNotificationService;

    public PlanResponse createPlan(String userId) {

        Plan plan = Plan.builder()
                .userId(userId)
                .build(); // @PrePersist at Plan Entity handles the rest

        planRepository.save(plan);
        return PlanResponse.from(plan);
    }

    public CheckoutSessionResponse startPremiumCheckout(String userId) {
        Plan plan = findPlanOrThrow(userId);

        ensureNotAlreadyPremium(plan);

        String checkoutUrl = paymentCheckoutProvider.createCheckoutSession(userId);

        return new CheckoutSessionResponse(checkoutUrl);
    }

    public PlanResponse upgradeToPremium(String userId, String stripeCustomerId, String stripeSubscriptionId) {
        Plan plan = findPlanOrThrow(userId);
        ensureNotAlreadyPremium(plan);
        plan.upgradeToPremium(stripeCustomerId, stripeSubscriptionId);
        planRepository.save(plan);
        return PlanResponse.from(plan);
    }

    public PlanStatusResponse getMyPlan(String userId) {
        Plan plan = findPlanOrThrow(userId);

        return PlanStatusResponse.from(plan);
    }

    @Transactional
    public void checkExpiredPlans() {
        List<Plan> expiredPlans = planRepository
                .findByExpiresAtBeforeAndPlanType(LocalDateTime.now(), PlanType.TRIAL);

        expiredPlans.forEach(plan -> {
            plan.downgradeToFree();
            planRepository.save(plan);
        });
    }

    @Override
    @Transactional
    public void handleCheckoutCompleted(String userId, String customerId, String subscriptionId) {
        log.info("Processing webhook checkout completed for userId: {}, subscriptionId: {}", userId, subscriptionId);

        try {
            upgradeToPremium(userId, customerId, subscriptionId);
            log.info("Successfully upgraded user {} to PREMIUM via webhook.", userId);
        } catch (Exception ex) {
            log.error("Failed to process checkout completion for userId: {}, subscriptionId: {}. Reason: {}",
                    userId, subscriptionId, ex.getMessage(), ex);

            // Re-throw so Controller returns 500 to trigger Stripe retry
            throw ex;
        }
    }

    /*If plan doesn't exist this method will add a trial plan on the first request
     * via PlanFirstAccessFilter */
    @Transactional
    public void ensurePlanExists(String userId) {
        if (!planRepository.existsByUserId(userId)) {
            try {
                Plan plan = Plan.builder()
                        .userId(userId)
                        .build();

                planRepository.saveAndFlush(plan);

            } catch (DataIntegrityViolationException ex) {
                log.info("Plan creation race condition detected for user: {}. Plan already persisted concurrently.", userId);
            }
        }
    }

    @Override
    @Transactional
    public void handleInvoicePaid(String customerId, String subscriptionId) {
        log.info("Processing webhook invoice.paid for customerId: {}, subscriptionId: {}", customerId, subscriptionId);

        planRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                plan -> {
                    if (!PlanType.PREMIUM.equals(plan.getPlanType())) {
                        plan.upgradeToPremium(customerId, subscriptionId);
                        planRepository.save(plan);
                        log.info("Plan reactivated to PREMIUM via invoice.paid for customerId: {}", customerId);
                    } else {
                        log.info("Renewal confirmed for customerId: {}. Plan is already PREMIUM.", customerId);
                    }
                },
                () -> log.warn("Invoice paid received, but no plan was found for customerId: {}. Skipping.", customerId)
        );
    }

    @Override
    public void handlePaymentFailed(String customerId, String subscriptionId) {
        log.warn("Payment failed webhook received for customerId: {}, subscriptionId: {}. No action taken on plan status.",
                customerId, subscriptionId);

        planRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                plan -> billingNotificationService.notifyPaymentFailed(customerId),
                () -> log.warn("Payment failed event received, but no plan was found for customerId: {}. Skipping notification.", customerId)
        );
    }

    @Override
    @Transactional
    public void handleSubscriptionDeleted(String customerId, String subscriptionId) {
        log.info("Processing webhook customer.subscription.deleted for customerId: {}, subscriptionId: {}", customerId, subscriptionId);

        planRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                plan -> {
                    plan.downgradeToFree();
                    planRepository.save(plan);
                    billingNotificationService.notifyCancellation(customerId);
                    log.info("Successfully downgraded plan to FREE for customerId: {}", customerId);
                },
                () -> log.warn("Subscription deleted event received, but no plan was found for customerId: {}. Skipping downgrade.", customerId)
        );
    }

    private void ensureNotAlreadyPremium(Plan plan) {
        if (plan.getPlanType().equals(PlanType.PREMIUM)) {
            throw new BusinessException("Plan is already Premium");
        }
    }

    private Plan findPlanOrThrow(String userId) {
        return planRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
    }
}
