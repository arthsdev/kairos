package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.anonymization.UserReferenceService;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.security.SecurityService;
import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.exception.ForbiddenException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.shared.pagination.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccurrenceServiceTest {

    @Mock
    private OccurrenceRepository occurrenceRepository;

    @Mock
    private SecurityService securityService;

    @Mock
    private OccurrenceActionsCalculator occurrenceActionsCalculator;

    @InjectMocks
    private OccurrenceService occurrenceService;

    @Mock
    private UserReferenceService userReferenceService;

    @Captor
    private ArgumentCaptor<Occurrence> occurrenceCaptor;

    private OccurrenceActions dummyActions;

    @BeforeEach
    void setUp() {
        dummyActions = new OccurrenceActions(true, true, true, true);
    }

    @Nested
    @DisplayName("Tests for createOccurrence()")
    class CreateOccurrence {

        @Test
        @DisplayName("Should create occurrence successfully")
        void shouldCreateOccurrenceSuccessfully() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "Landslide on main road", "Heavy rain caused landslide", OccurrenceCategory.LANDSLIDE,
                    OccurrenceSeverity.HIGH, -23.55, -46.63, "https://image.com/landslide.jpg", "city-123"
            );
            String userId = "user-123";

            when(securityService.isAdmin()).thenReturn(false);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), eq(userId), eq(false)))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.createOccurrence(request, userId);

            verify(occurrenceRepository).save(occurrenceCaptor.capture());
            Occurrence savedOccurrence = occurrenceCaptor.getValue();

            assertThat(savedOccurrence.getTitle()).isEqualTo(request.title());
            assertThat(savedOccurrence.getCategory()).isEqualTo(OccurrenceCategory.LANDSLIDE);
            assertThat(savedOccurrence.getUserId()).isEqualTo(userId);
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tests for getOccurrences()")
    class GetOccurrences {

        @Test
        @DisplayName("Should return paginated occurrences filtered by status with admin privileges")
        void shouldReturnPaginatedOccurrencesWithStatusAsAdmin() {
            Pageable pageable = PageRequest.of(0, 10);
            Occurrence occurrence = Occurrence.builder().status(OccurrenceStatus.VERIFIED).build();
            Page<Occurrence> page = new PageImpl<>(List.of(occurrence));

            when(securityService.getCurrentUserId()).thenReturn("admin-123");
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceRepository.findAllByStatusAndDeletedAtIsNull(OccurrenceStatus.VERIFIED, pageable))
                    .thenReturn(page);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), eq("admin-123"), eq(true)))
                    .thenReturn(dummyActions);
            when(userReferenceService.getDisplayId(any()))
                    .thenReturn("#12345");

            PaginatedResponse<OccurrenceResponse> response = occurrenceService.getOccurrences(OccurrenceStatus.VERIFIED, pageable);

            assertThat(response.data()).hasSize(1);
            assertThat(response.data().getFirst().actions()).isEqualTo(dummyActions);
            verify(occurrenceRepository).findAllByStatusAndDeletedAtIsNull(OccurrenceStatus.VERIFIED, pageable);
        }

        @Test
        @DisplayName("Should return all paginated occurrences when status is null with admin privileges")
        void shouldReturnAllPaginatedOccurrencesWhenStatusIsNullAsAdmin() {
            Pageable pageable = PageRequest.of(0, 10);
            Occurrence occurrence = Occurrence.builder().status(OccurrenceStatus.PENDING).build();
            Page<Occurrence> page = new PageImpl<>(List.of(occurrence));

            when(securityService.getCurrentUserId()).thenReturn("admin-123");
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceRepository.findAllByDeletedAtIsNull(pageable))
                    .thenReturn(page);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), eq("admin-123"), eq(true)))
                    .thenReturn(dummyActions);
            when(userReferenceService.getDisplayId(any()))
                    .thenReturn("#12345");

            PaginatedResponse<OccurrenceResponse> response = occurrenceService.getOccurrences(null, pageable);

            assertThat(response.data()).hasSize(1);
            assertThat(response.data().getFirst().actions()).isEqualTo(dummyActions);
            verify(occurrenceRepository).findAllByDeletedAtIsNull(pageable);
        }
    }

    @Nested
    @DisplayName("Tests for getMyOccurrences()")
    class GetMyOccurrences {

        @Test
        @DisplayName("Should return my occurrences filtered by status when status is provided")
        void shouldReturnMyOccurrencesWithStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Occurrence occurrence = Occurrence.builder().userId("user-123").status(OccurrenceStatus.PENDING).build();
            Page<Occurrence> page = new PageImpl<>(List.of(occurrence));

            when(occurrenceRepository.findAllByUserIdAndStatusAndDeletedAtIsNull("user-123", OccurrenceStatus.PENDING, pageable))
                    .thenReturn(page);
            when(securityService.isAdmin()).thenReturn(false);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), eq("user-123"), eq(false)))
                    .thenReturn(dummyActions);

            PaginatedResponse<OccurrenceResponse> response = occurrenceService.getMyOccurrences("user-123", OccurrenceStatus.PENDING, pageable);

            assertThat(response.data()).hasSize(1);
        }

        @Test
        @DisplayName("Should return all my occurrences when status is null")
        void shouldReturnAllMyOccurrencesWhenStatusIsNull() {
            Pageable pageable = PageRequest.of(0, 10);
            Occurrence occurrence = Occurrence.builder().userId("user-123").build();
            Page<Occurrence> page = new PageImpl<>(List.of(occurrence));

            when(occurrenceRepository.findAllByUserIdAndDeletedAtIsNull("user-123", pageable))
                    .thenReturn(page);
            when(securityService.isAdmin()).thenReturn(false);

            occurrenceService.getMyOccurrences("user-123", null, pageable);

            verify(occurrenceRepository).findAllByUserIdAndDeletedAtIsNull("user-123", pageable);
        }
    }

    @Nested
    @DisplayName("Tests for findVerifiedByCityId()")
    class FindVerifiedByCityId {

        @Test
        @DisplayName("Should return list of summaries for verified/resolved occurrences in city")
        void shouldReturnSummaries() {
            String cityId = "city-123";
            Occurrence occurrence = Occurrence.builder()
                    .category(OccurrenceCategory.FLOOD)
                    .severity(OccurrenceSeverity.HIGH)
                    .status(OccurrenceStatus.VERIFIED)
                    .cityId(cityId)
                    .build();

            when(occurrenceRepository.findByCityIdAndStatusInAndDeletedAtIsNull(cityId, List.of(OccurrenceStatus.VERIFIED, OccurrenceStatus.RESOLVED)))
                    .thenReturn(List.of(occurrence));

            List<OccurrenceSummary> result = occurrenceService.findVerifiedByCityId(cityId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().cityId()).isEqualTo(cityId);
            assertThat(result.getFirst().category()).isEqualTo(OccurrenceCategory.FLOOD);
        }
    }

    @Nested
    @DisplayName("Tests for updateOccurrence()")
    class UpdateOccurrence {

        @Test
        @DisplayName("Should update occurrence successfully when user is the owner")
        void shouldUpdateSuccessfullyAsOwner() {
            String id = "occ-123";
            UpdateOccurrenceRequest request = new UpdateOccurrenceRequest("New Title", "New Desc");
            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .userId("user-123")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("user-123");
            when(securityService.isAdmin()).thenReturn(false);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.updateOccurrence(id, request);

            verify(occurrenceRepository).save(occurrence);
            assertThat(occurrence.getTitle()).isEqualTo("New Title");
            assertThat(occurrence.getDescription()).isEqualTo("New Desc");
            assertThat(response.actions()).isEqualTo(dummyActions);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when occurrence does not exist")
        void shouldThrowNotFound() {
            when(occurrenceRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> occurrenceService.updateOccurrence("invalid", new UpdateOccurrenceRequest("T", "D")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Occurrence not found");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user is not the owner and not an admin")
        void shouldThrowForbidden() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder().id(id).userId("owner-user").status(OccurrenceStatus.PENDING).build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("stranger-user");
            when(securityService.isAdmin()).thenReturn(false);

            assertThatThrownBy(() -> occurrenceService.updateOccurrence(id, new UpdateOccurrenceRequest("T", "D")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can't update the occurrence");
        }

        @Test
        @DisplayName("Should update occurrence successfully when user is admin but not the owner")
        void shouldUpdateSuccessfullyAsAdmin() {
            String id = "occ-123";
            UpdateOccurrenceRequest request = new UpdateOccurrenceRequest("New Title", "New Desc");

            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .userId("owner-user")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("admin-user");
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.updateOccurrence(id, request);

            verify(occurrenceRepository).save(occurrence);
            assertThat(occurrence.getTitle()).isEqualTo("New Title");
            assertThat(occurrence.getDescription()).isEqualTo("New Desc");
            assertThat(response.actions()).isEqualTo(dummyActions);
        }
    }

    @Nested
    @DisplayName("Tests for verifyOccurrence()")
    class VerifyOccurrence {

        @Test
        @DisplayName("Should verify occurrence successfully when user is admin")
        void shouldVerifySuccessfullyAsAdmin() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .status(OccurrenceStatus.PENDING)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.verifyOccurrence(id);

            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.VERIFIED);
            verify(occurrenceRepository).save(occurrence);
            assertThat(response.actions()).isEqualTo(dummyActions);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when non-admin tries to verify")
        void shouldThrowForbiddenForNonAdmin() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder().id(id).status(OccurrenceStatus.PENDING).build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.isAdmin()).thenReturn(false);

            assertThatThrownBy(() -> occurrenceService.verifyOccurrence(id))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only admins can verify occurrences");
        }
    }

    @Nested
    @DisplayName("Tests for resolveOccurrence()")
    class ResolveOccurrence {

        @Test
        @DisplayName("Should resolve occurrence successfully when user is admin")
        void shouldResolveSuccessfullyAsAdmin() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .status(OccurrenceStatus.VERIFIED)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.resolveOccurrence(id);

            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.RESOLVED);
            verify(occurrenceRepository).save(occurrence);
            assertThat(response.actions()).isEqualTo(dummyActions);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when non-admin tries to resolve")
        void shouldThrowForbiddenForNonAdmin() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder().id(id).status(OccurrenceStatus.VERIFIED).build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.isAdmin()).thenReturn(false);

            assertThatThrownBy(() -> occurrenceService.resolveOccurrence(id))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only admins can resolve occurrences");
        }
    }

    @Nested
    @DisplayName("Tests for deleteOccurrence()")
    class DeleteOccurrence {

        @Test
        @DisplayName("Should delete occurrence successfully when user is the owner")
        void shouldDeleteAsOwner() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .userId("user-123")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("user-123");
            when(securityService.isAdmin()).thenReturn(false);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.deleteOccurrence(id);

            assertThat(occurrence.isDeleted()).isTrue();
            verify(occurrenceRepository).save(occurrence);
            assertThat(response.actions()).isEqualTo(dummyActions);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user is not owner and not admin")
        void shouldThrowForbiddenForNonOwner() {
            String id = "occ-123";
            Occurrence occurrence = Occurrence.builder().id(id).userId("owner-user").status(OccurrenceStatus.PENDING).build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("stranger-user");
            when(securityService.isAdmin()).thenReturn(false);

            assertThatThrownBy(() -> occurrenceService.deleteOccurrence(id))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can't delete the occurrence");
        }

        @Test
        @DisplayName("Should delete occurrence successfully when user is admin but not the owner")
        void shouldDeleteSuccessfullyAsAdmin() {
            String id = "occ-123";

            Occurrence occurrence = Occurrence.builder()
                    .id(id)
                    .userId("owner-user")
                    .status(OccurrenceStatus.PENDING)
                    .build();

            when(occurrenceRepository.findById(id)).thenReturn(Optional.of(occurrence));
            when(securityService.getCurrentUserId()).thenReturn("admin-user");
            when(securityService.isAdmin()).thenReturn(true);
            when(occurrenceActionsCalculator.calculate(any(Occurrence.class), any(), anyBoolean()))
                    .thenReturn(dummyActions);

            OccurrenceResponse response = occurrenceService.deleteOccurrence(id);

            verify(occurrenceRepository).save(occurrence);
            assertThat(occurrence.isDeleted()).isTrue();
            assertThat(response.actions()).isEqualTo(dummyActions);
        }
    }
}