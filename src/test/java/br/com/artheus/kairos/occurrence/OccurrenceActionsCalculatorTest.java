package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrenceActionsCalculatorTest {

    private final OccurrenceActionsCalculator calculator = new OccurrenceActionsCalculator();

    private static final String OWNER_ID = "user-owner";
    private static final String STRANGER_ID = "user-stranger";

    @Nested
    @DisplayName("Tests for Regular User who IS the owner")
    class RegularUserOwner {

        @Test
        @DisplayName("Should allow edit/delete but deny verify/resolve when status is PENDING")
        void pendingOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, false);

            assertThat(actions.canEdit()).isTrue();
            assertThat(actions.canDelete()).isTrue();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should deny all actions when status is VERIFIED (Finalized)")
        void verifiedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, false);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should deny all actions when occurrence is soft-deleted")
        void deletedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, false);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for Regular User who is NOT the owner")
    class RegularUserStranger {

        @Test
        @DisplayName("Should deny all actions for any status")
        void anyOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, false);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should deny all actions when occurrence is soft-deleted")
        void deletedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, false);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for Admin who IS the owner")
    class AdminOwner {

        @Test
        @DisplayName("Should allow edit, delete, and verify when status is PENDING")
        void pendingOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, true);

            assertThat(actions.canEdit()).isTrue();
            assertThat(actions.canDelete()).isTrue();
            assertThat(actions.canVerify()).isTrue();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should allow resolve but deny edit, delete, and verify when status is VERIFIED")
        void verifiedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, true);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isTrue();
        }

        @Test
        @DisplayName("Should deny all actions when occurrence is soft-deleted")
        void deletedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, OWNER_ID, true);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for Admin who is NOT the owner")
    class AdminStranger {

        @Test
        @DisplayName("Should allow edit, delete, and verify when status is PENDING even if not the owner")
        void pendingOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, true);

            assertThat(actions.canEdit()).isTrue();
            assertThat(actions.canDelete()).isTrue();
            assertThat(actions.canVerify()).isTrue();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should allow resolve but deny edit/delete/verify when status is VERIFIED")
        void verifiedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, true);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isTrue();
        }

        @Test
        @DisplayName("Should deny everything if the occurrence is RESOLVED")
        void resolvedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.RESOLVED)
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, true);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }

        @Test
        @DisplayName("Should deny everything if the occurrence is soft-deleted")
        void deletedOccurrence() {
            Occurrence occurrence = Occurrence.builder()
                    .userId(OWNER_ID)
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            OccurrenceActions actions = calculator.calculate(occurrence, STRANGER_ID, true);

            assertThat(actions.canEdit()).isFalse();
            assertThat(actions.canDelete()).isFalse();
            assertThat(actions.canVerify()).isFalse();
            assertThat(actions.canResolve()).isFalse();
        }
    }
}