package br.com.artheus.kairos.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Plan Domain Entity Unit Tests")
class PlanTest {

    @Nested
    @DisplayName("Tests for isExpired")
    class IsExpiredTests {

        @Test
        @DisplayName("Should return false when expiresAt is null")
        void shouldReturnFalseWhenExpiresAtIsNull() {
            Plan plan = Plan.builder()
                    .expiresAt(null)
                    .build();

            assertThat(plan.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should return false when current time is before expiresAt")
        void shouldReturnFalseWhenPlanIsStillValid() {
            Plan plan = Plan.builder()
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            assertThat(plan.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should return true when current time is after expiresAt")
        void shouldReturnTrueWhenPlanIsExpired() {
            Plan plan = Plan.builder()
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            assertThat(plan.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests for Plan State Transitions")
    class PlanStateTransitionsTests {

        @Test
        @DisplayName("Should correctly upgrade to premium state and extend expiration by 30 days")
        void shouldUpgradeToPremiumSuccessfully() {
            Plan plan = Plan.builder()
                    .planType(PlanType.FREE)
                    .cityLimit(1)
                    .build();

            plan.upgradeToPremium();

            assertThat(plan.getPlanType()).isEqualTo(PlanType.PREMIUM);
            assertThat(plan.getCityLimit()).isEqualTo(5);
            assertThat(plan.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(plan.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
        }

        @Test
        @DisplayName("Should correctly downgrade to free state and reduce city limit to 1")
        void shouldDowngradeToFreeSuccessfully() {
            Plan plan = Plan.builder()
                    .planType(PlanType.PREMIUM)
                    .cityLimit(5)
                    .build();

            plan.downgradeToFree();

            assertThat(plan.getPlanType()).isEqualTo(PlanType.FREE);
            assertThat(plan.getCityLimit()).isEqualTo(1);
            assertThat(plan.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("Tests for hasReachedCityLimit")
    class CityLimitTests {

        @Test
        @DisplayName("Should return true when current count is equal to city limit")
        void shouldReturnTrueWhenCountEqualsLimit() {
            Plan plan = Plan.builder().cityLimit(5).build();

            assertThat(plan.hasReachedCityLimit(5)).isTrue();
        }

        @Test
        @DisplayName("Should return true when current count exceeds city limit")
        void shouldReturnTrueWhenCountExceedsLimit() {
            Plan plan = Plan.builder().cityLimit(5).build();

            assertThat(plan.hasReachedCityLimit(6)).isTrue();
        }

        @Test
        @DisplayName("Should return false when current count is below city limit")
        void shouldReturnFalseWhenCountIsBelowLimit() {
            Plan plan = Plan.builder().cityLimit(5).build();

            assertThat(plan.hasReachedCityLimit(4)).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for JPA Lifecycle Hooks (@PrePersist and @PreUpdate)")
    class JpaLifecycleHooksTests {

        @Test
        @DisplayName("Should initialize trial status and 7 days expiration on @PrePersist")
        void shouldApplyOnCreateLifecycleHook() {
            Plan plan = new Plan();

            plan.onCreate();

            assertThat(plan.getPlanType()).isEqualTo(PlanType.TRIAL);
            assertThat(plan.getCityLimit()).isEqualTo(5);
            assertThat(plan.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(plan.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        }

        @Test
        @DisplayName("Should update timestamp on @PreUpdate")
        void shouldApplyOnUpdateLifecycleHook() {
            Plan plan = Plan.builder().build();

            plan.onUpdate();

            assertThat(plan.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}