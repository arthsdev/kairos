package br.com.artheus.kairos.plan;

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

    @Mock
    private PlanRepository planRepository;

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
    @DisplayName("Tests for upgradeToPremium")
    class UpgradeToPremiumTests {

        @Test
        @DisplayName("Should successfully upgrade to PREMIUM when current plan is FREE")
        void shouldUpgradeToPremiumWhenPlanIsFree() {
            String userId = "user-free";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.FREE).cityLimit(1).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            PlanResponse response = planService.upgradeToPremium(userId);

            verify(planRepository, times(1)).save(currentPlan);
            assertThat(currentPlan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(currentPlan.getCityLimit()).isEqualTo(5);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should successfully upgrade to PREMIUM when current plan is TRIAL")
        void shouldUpgradeToPremiumWhenPlanIsTrial() {
            String userId = "user-trial";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.TRIAL).cityLimit(5).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            PlanResponse response = planService.upgradeToPremium(userId);

            verify(planRepository, times(1)).save(currentPlan);
            assertThat(currentPlan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw BusinessException when plan is already PREMIUM")
        void shouldThrowExceptionWhenPlanIsAlreadyPremium() {
            String userId = "user-premium";
            Plan currentPlan = Plan.builder().userId(userId).planType(PlanType.PREMIUM).build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(currentPlan));

            assertThatThrownBy(() -> planService.upgradeToPremium(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plan is already Premium");

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user has no active plan")
        void shouldThrowExceptionWhenPlanNotFound() {
            String userId = "user-ghost";
            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.upgradeToPremium(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");

            verify(planRepository, never()).save(any());
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
            when(planRepository.findByExpiresAtBeforeAndPlanTypeNot(any(LocalDateTime.class), eq(PlanType.FREE)))
                    .thenReturn(Collections.emptyList());

            planService.checkExpiredPlans();

            verify(planRepository, never()).save(any(Plan.class));
        }

        @Test
        @DisplayName("Should iterate, downgrade each expired plan to FREE, and persist updates")
        void shouldDowngradeAllExpiredPlansFound() {
            Plan planOne = spy(Plan.builder().planType(PlanType.PREMIUM).cityLimit(5).build());
            Plan planTwo = spy(Plan.builder().planType(PlanType.TRIAL).cityLimit(5).build());
            List<Plan> expiredPlans = List.of(planOne, planTwo);

            when(planRepository.findByExpiresAtBeforeAndPlanTypeNot(any(LocalDateTime.class), eq(PlanType.FREE)))
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
        @DisplayName("Should create and save a default trial plan when user does not have one")
        void shouldCreatePlanWhenUserHasNone() {
            String userId = "user-new";
            when(planRepository.existsByUserId(userId)).thenReturn(false);

            planService.ensurePlanExists(userId);

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(planRepository, times(1)).save(planCaptor.capture());
            assertThat(planCaptor.getValue().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should do nothing when the plan already exists for the user ID")
        void shouldDoNothingWhenPlanAlreadyExists() {
            String userId = "user-existing";
            when(planRepository.existsByUserId(userId)).thenReturn(true);

            planService.ensurePlanExists(userId);

            verify(planRepository, never()).save(any(Plan.class));
        }
    }
}