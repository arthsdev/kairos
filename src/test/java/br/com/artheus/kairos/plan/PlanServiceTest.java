package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.contract.payment.PaymentCheckoutProvider;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService Unit Tests")
class PlanServiceTest {

    private static final String DEFAULT_CUSTOMER_ID = "cus_test_123";
    private static final String DEFAULT_SUBSCRIPTION_ID = "sub_test_456";

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PaymentCheckoutProvider paymentCheckoutProvider;

    @Mock
    private BillingNotificationService billingNotificationService;

    @InjectMocks
    private PlanService planService;

    @Nested
    @DisplayName("Tests for createPlan")
    class CreatePlanTests {

        @Test
        @DisplayName("Should build and save a new plan for the given user ID")
        void shouldCreatePlanSuccessfully() {
            String userId = "user-123";

            PlanResponse response = planService.createPlan(userId);

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(planRepository, times(1)).save(planCaptor.capture());

            Plan savedPlan = planCaptor.getValue();
            assertThat(savedPlan.getUserId()).isEqualTo(userId);
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tests for startPremiumCheckout")
    class StartPremiumCheckoutTests {

        @Test
        @DisplayName("Should create checkout session URL successfully when plan exists and is not PREMIUM")
        void shouldStartPremiumCheckoutSuccessfully() {
            String userId = "user-123";
            String expectedCheckoutUrl = "https://checkout.stripe.com/pay/cs_test_123";
            Plan plan = Plan.builder().userId(userId).planType(PlanType.FREE).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(paymentCheckoutProvider.createCheckoutSession(userId)).thenReturn(expectedCheckoutUrl);

            CheckoutSessionResponse response = planService.startPremiumCheckout(userId);

            assertThat(response).isNotNull();
            assertThat(response.checkoutUrl()).isEqualTo(expectedCheckoutUrl);
            verify(paymentCheckoutProvider, times(1)).createCheckoutSession(userId);
        }

        @Test
        @DisplayName("Should throw BusinessException when plan is already PREMIUM during checkout start")
        void shouldThrowExceptionWhenStartingCheckoutForAlreadyPremiumPlan() {
            String userId = "user-premium";
            Plan plan = Plan.builder().userId(userId).planType(PlanType.PREMIUM).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> planService.startPremiumCheckout(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plan is already Premium");

            verify(paymentCheckoutProvider, never()).createCheckoutSession(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user has no plan during checkout start")
        void shouldThrowExceptionWhenPlanNotFoundOnCheckoutStart() {
            String userId = "user-ghost";
            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.startPremiumCheckout(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");

            verify(paymentCheckoutProvider, never()).createCheckoutSession(any());
        }
    }

    @Nested
    @DisplayName("Tests for upgradeToPremium")
    class UpgradeToPremiumTests {

        @Test
        @DisplayName("Should successfully upgrade to PREMIUM and persist Stripe IDs when current plan is FREE")
        void shouldUpgradeToPremiumWhenPlanIsFree() {
            String userId = "user-free";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.FREE).cityLimit(1).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            PlanResponse response = planService.upgradeToPremium(userId, DEFAULT_CUSTOMER_ID, DEFAULT_SUBSCRIPTION_ID);

            verify(planRepository, times(1)).save(currentPlan);
            assertThat(currentPlan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(currentPlan.getCityLimit()).isEqualTo(5);
            assertThat(currentPlan.getStripeCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
            assertThat(currentPlan.getStripeSubscriptionId()).isEqualTo(DEFAULT_SUBSCRIPTION_ID);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should successfully upgrade to PREMIUM and persist Stripe IDs when current plan is TRIAL")
        void shouldUpgradeToPremiumWhenPlanIsTrial() {
            String userId = "user-trial";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.TRIAL).cityLimit(5).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            PlanResponse response = planService.upgradeToPremium(userId, DEFAULT_CUSTOMER_ID, DEFAULT_SUBSCRIPTION_ID);

            verify(planRepository, times(1)).save(currentPlan);
            assertThat(currentPlan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(currentPlan.getStripeCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
            assertThat(currentPlan.getStripeSubscriptionId()).isEqualTo(DEFAULT_SUBSCRIPTION_ID);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw BusinessException when plan is already PREMIUM")
        void shouldThrowExceptionWhenPlanIsAlreadyPremium() {
            String userId = "user-premium";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.PREMIUM).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            assertThatThrownBy(() -> planService.upgradeToPremium(userId, DEFAULT_CUSTOMER_ID, DEFAULT_SUBSCRIPTION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plan is already Premium");

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user has no active plan")
        void shouldThrowExceptionWhenPlanNotFound() {
            String userId = "user-ghost";
            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.upgradeToPremium(userId, DEFAULT_CUSTOMER_ID, DEFAULT_SUBSCRIPTION_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");

            verify(planRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for handleCheckoutCompleted")
    class HandleCheckoutCompletedTests {

        @Test
        @DisplayName("Should successfully upgrade user plan to PREMIUM and persist customer and subscription IDs on checkout completed")
        void shouldUpgradeUserToPremiumOnCheckoutCompleted() {
            String userId = "user-123";
            String customerId = "cus_webhook_123";
            String subscriptionId = "sub_webhook_456";

            Plan plan = Plan.builder()
                    .userId(userId)
                    .planType(PlanType.FREE)
                    .cityLimit(1)
                    .build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));

            planService.handleCheckoutCompleted(userId, customerId, subscriptionId);

            verify(planRepository, times(1)).save(plan);
            assertThat(plan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(plan.getStripeCustomerId()).isEqualTo(customerId);
            assertThat(plan.getStripeSubscriptionId()).isEqualTo(subscriptionId);
        }

        @Test
        @DisplayName("Should rethrow exception when upgrade fails to trigger webhook retry")
        void shouldRethrowExceptionWhenUpgradeFails() {
            String userId = "user-ghost";
            String customerId = "cus_webhook_123";
            String subscriptionId = "sub_webhook_456";

            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.handleCheckoutCompleted(userId, customerId, subscriptionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");

            verify(planRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for handleInvoicePaid")
    class HandleInvoicePaidTests {

        @Test
        @DisplayName("Should log renewal confirmation without changes when plan is already PREMIUM")
        void shouldConfirmRenewalWhenAlreadyPremium() {
            String customerId = "cus_123";
            String subscriptionId = "sub_456";
            Plan plan = Plan.builder().planType(PlanType.PREMIUM).build();

            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(plan));

            planService.handleInvoicePaid(customerId, subscriptionId);

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reactivate plan to PREMIUM when plan exists but is not PREMIUM")
        void shouldReactivatePlanWhenNotPremium() {
            String customerId = "cus_123";
            String subscriptionId = "sub_456";
            Plan plan = Plan.builder().planType(PlanType.FREE).cityLimit(1).build();

            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(plan));

            planService.handleInvoicePaid(customerId, subscriptionId);

            verify(planRepository, times(1)).save(plan);
            assertThat(plan.getPlanType()).isEqualTo(PlanType.PREMIUM);
        }

        @Test
        @DisplayName("Should do nothing when no plan is found for the customerId")
        void shouldDoNothingWhenPlanNotFound() {
            String customerId = "cus_ghost";
            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.empty());

            planService.handleInvoicePaid(customerId, "sub_456");

            verify(planRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for handlePaymentFailed")
    class HandlePaymentFailedTests {

        @Test
        @DisplayName("Should notify billing failure when plan is found")
        void shouldNotifyWhenPlanFound() {
            String customerId = "cus_123";
            Plan plan = Plan.builder().planType(PlanType.PREMIUM).build();

            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(plan));

            planService.handlePaymentFailed(customerId, "sub_456");

            verify(billingNotificationService, times(1)).notifyPaymentFailed(customerId);
        }

        @Test
        @DisplayName("Should not notify when no plan is found for the customerId")
        void shouldNotNotifyWhenPlanNotFound() {
            String customerId = "cus_ghost";
            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.empty());

            planService.handlePaymentFailed(customerId, "sub_456");

            verify(billingNotificationService, never()).notifyPaymentFailed(any());
        }
    }

    @Nested
    @DisplayName("Tests for handleSubscriptionDeleted")
    class HandleSubscriptionDeletedTests {

        @Test
        @DisplayName("Should downgrade plan to FREE and notify when plan is found")
        void shouldDowngradeToFreeAndNotifyWhenPlanFound() {
            String customerId = "cus_123";
            Plan plan = spy(Plan.builder().planType(PlanType.PREMIUM).cityLimit(5).stripeSubscriptionId("sub_456").build());

            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(plan));

            planService.handleSubscriptionDeleted(customerId, "sub_456");

            verify(plan, times(1)).downgradeToFree();
            verify(planRepository, times(1)).save(plan);
            verify(billingNotificationService, times(1)).notifyCancellation(customerId);
            assertThat(plan.getPlanType()).isEqualTo(PlanType.FREE);
        }

        @Test
        @DisplayName("Should not notify when no plan is found for the customerId")
        void shouldNotNotifyWhenPlanNotFoundOnDeletion() {
            String customerId = "cus_ghost";
            when(planRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.empty());

            planService.handleSubscriptionDeleted(customerId, "sub_456");

            verify(planRepository, never()).save(any());
            verify(billingNotificationService, never()).notifyCancellation(any());
        }
    }

    @Nested
    @DisplayName("Tests for getMyPlan")
    class GetMyPlanTests {

        @Test
        @DisplayName("Should return PlanStatusResponse when plan exists")
        void shouldReturnPlanStatusSuccessfully() {
            String userId = "user-123";
            Plan plan = Plan.builder().userId(userId).planType(PlanType.FREE).cityLimit(1).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));

            PlanStatusResponse response = planService.getMyPlan(userId);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when fetching status for non-existent plan")
        void shouldThrowExceptionWhenPlanDoesNotExist() {
            String userId = "user-ghost";
            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.getMyPlan(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");
        }
    }

    @Nested
    @DisplayName("Tests for checkExpiredPlans")
    class CheckExpiredPlansTests {

        @Test
        @DisplayName("Should do nothing when no expired plans are returned by the repository")
        void shouldDoNothingWhenNoPlansAreExpired() {
            when(planRepository.findByExpiresAtBeforeAndPlanType(any(LocalDateTime.class), eq(PlanType.TRIAL)))
                    .thenReturn(Collections.emptyList());

            planService.checkExpiredPlans();

            verify(planRepository, never()).save(any(Plan.class));
        }

        @Test
        @DisplayName("Should iterate, downgrade each expired TRIAL plan to FREE, and persist updates")
        void shouldDowngradeAllExpiredTrialPlansFound() {
            Plan planOne = spy(Plan.builder().planType(PlanType.TRIAL).cityLimit(5).build());
            Plan planTwo = spy(Plan.builder().planType(PlanType.TRIAL).cityLimit(5).build());
            List<Plan> expiredPlans = List.of(planOne, planTwo);

            when(planRepository.findByExpiresAtBeforeAndPlanType(any(LocalDateTime.class), eq(PlanType.TRIAL)))
                    .thenReturn(expiredPlans);

            planService.checkExpiredPlans();

            verify(planOne, times(1)).downgradeToFree();
            verify(planTwo, times(1)).downgradeToFree();

            verify(planRepository, times(1)).save(planOne);
            verify(planRepository, times(1)).save(planTwo);

            assertThat(planOne.getPlanType()).isEqualTo(PlanType.FREE);
            assertThat(planTwo.getPlanType()).isEqualTo(PlanType.FREE);
        }
    }

    @Nested
    @DisplayName("Tests for ensurePlanExists")
    class EnsurePlanExistsTests {

        @Test
        @DisplayName("Should create and saveAndFlush a default trial plan when user does not have one")
        void shouldCreatePlanWhenUserHasNone() {
            String userId = "user-new";
            when(planRepository.existsByUserId(userId)).thenReturn(false);

            planService.ensurePlanExists(userId);

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(planRepository, times(1)).saveAndFlush(planCaptor.capture());
            assertThat(planCaptor.getValue().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should do nothing when the plan already exists for the user ID")
        void shouldDoNothingWhenPlanAlreadyExists() {
            String userId = "user-existing";
            when(planRepository.existsByUserId(userId)).thenReturn(true);

            planService.ensurePlanExists(userId);

            verify(planRepository, never()).saveAndFlush(any(Plan.class));
        }

        @Test
        @DisplayName("Should handle race condition and complete gracefully when unique constraint is triggered")
        void shouldTreatRaceConditionAsNoOpWhenConstraintViolationOccurs() {
            String userId = "user-concurrent";
            when(planRepository.existsByUserId(userId)).thenReturn(false);

            when(planRepository.saveAndFlush(any(Plan.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate key value violates unique constraint"));

            planService.ensurePlanExists(userId);

            verify(planRepository, times(1)).saveAndFlush(any(Plan.class));
        }
    }
}