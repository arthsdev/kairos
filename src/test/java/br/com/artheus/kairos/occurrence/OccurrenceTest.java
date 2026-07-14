package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceTest {

    @Nested
    @DisplayName("Tests for verify() method")
    class VerifyMethod {

        @Test
        @DisplayName("Should verify occurrence successfully when status is PENDING")
        void shouldVerifyOccurrenceSuccessfully() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.PENDING)
                    .build();

            occurrence.verify();

            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.VERIFIED);
        }

        @Test
        @DisplayName("Should throw BusinessException when verifying non-pending occurrence")
        void shouldThrowExceptionWhenNotPending() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.RESOLVED)
                    .build();

            assertThatThrownBy(occurrence::verify)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Only pending occurrences can be verified");
        }

        @Test
        @DisplayName("Should throw BusinessException when occurrence is deleted")
        void shouldThrowExceptionWhenDeleted() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(occurrence::verify)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot perform this action on a deleted occurrence.");
        }
    }

    @Nested
    @DisplayName("Tests for resolve() method")
    class ResolveMethod {

        @Test
        @DisplayName("Should resolve occurrence successfully when status is VERIFIED")
        void shouldResolveSuccessfully() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            occurrence.resolve();

            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.RESOLVED);
        }

        @Test
        @DisplayName("Should throw BusinessException when status is not VERIFIED (e.g. PENDING)")
        void shouldThrowExceptionWhenNotVerified() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.PENDING)
                    .build();

            assertThatThrownBy(occurrence::resolve)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Only verified occurrences can be resolved");
        }

        @Test
        @DisplayName("Should throw BusinessException when resolving a deleted occurrence")
        void shouldThrowExceptionWhenDeleted() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.VERIFIED)
                    .deletedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(occurrence::resolve)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot perform this action on a deleted occurrence.");
        }
    }

    @Nested
    @DisplayName("Tests for changeTitle() method")
    class ChangeTitleMethod {

        @Test
        @DisplayName("Should change title successfully when occurrence is active and PENDING")
        void shouldChangeTitleSuccessfully() {
            Occurrence occurrence = Occurrence.builder()
                    .title("Old Title")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            occurrence.changeTitle("New Title");

            assertThat(occurrence.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to change title of a deleted occurrence")
        void shouldThrowExceptionWhenDeleted() {
            Occurrence occurrence = Occurrence.builder()
                    .title("Old Title")
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(() -> occurrence.changeTitle("New Title"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot perform this action on a deleted occurrence.");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to change title of a finalized occurrence")
        void shouldThrowExceptionWhenFinalized() {
            Occurrence occurrence = Occurrence.builder()
                    .title("Old Title")
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            assertThatThrownBy(() -> occurrence.changeTitle("New Title"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot update the occurrence because it has already been finalized.");
        }
    }

    @Nested
    @DisplayName("Tests for changeDescription() method")
    class ChangeDescriptionMethod {

        @Test
        @DisplayName("Should change description successfully when occurrence is active and PENDING")
        void shouldChangeDescriptionSuccessfully() {
            Occurrence occurrence = Occurrence.builder()
                    .description("Old Description")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            occurrence.changeDescription("New Description");

            assertThat(occurrence.getDescription()).isEqualTo("New Description");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to change description of a deleted occurrence")
        void shouldThrowExceptionWhenDeleted() {
            Occurrence occurrence = Occurrence.builder()
                    .description("Old Description")
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(() -> occurrence.changeDescription("New Description"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot perform this action on a deleted occurrence.");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to change description of a finalized occurrence")
        void shouldThrowExceptionWhenFinalized() {
            Occurrence occurrence = Occurrence.builder()
                    .description("Old Description")
                    .status(OccurrenceStatus.RESOLVED)
                    .build();

            assertThatThrownBy(() -> occurrence.changeDescription("New Description"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot update the occurrence because it has already been finalized.");
        }
    }

    @Nested
    @DisplayName("Tests for delete() method")
    class DeleteMethod {

        @Test
        @DisplayName("Should mark occurrence as deleted successfully")
        void shouldDeleteSuccessfully() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.PENDING)
                    .build();

            occurrence.delete();

            assertThat(occurrence.isDeleted()).isTrue();
            assertThat(occurrence.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw BusinessException when already deleted")
        void shouldThrowExceptionIfAlreadyDeleted() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.PENDING)
                    .deletedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(occurrence::delete)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("You can't delete this occurrence.");
        }

        @Test
        @DisplayName("Should throw BusinessException when occurrence is finalized (VERIFIED)")
        void shouldThrowExceptionIfFinalizedVerified() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            assertThatThrownBy(occurrence::delete)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot delete the occurrence because it has already been finalized.");
        }
    }

    @Nested
    @DisplayName("Tests for JPA Lifecycle (@PrePersist)")
    class JpaLifecycle {

        @Test
        @DisplayName("Should populate createdAt and default status to PENDING on onCreate")
        void shouldPopulateFieldsOnCreate() {
            Occurrence occurrence = Occurrence.builder().build();

            occurrence.onCreate();

            assertThat(occurrence.getCreatedAt()).isNotNull();
            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.PENDING);
        }

        @Test
        @DisplayName("Should populate createdAt but keep existing status if already provided")
        void shouldKeepExistingStatusOnCreate() {
            Occurrence occurrence = Occurrence.builder()
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            occurrence.onCreate();

            assertThat(occurrence.getCreatedAt()).isNotNull();
            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.VERIFIED);
        }
    }
}