package br.com.artheus.kairos.anonymization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserReferenceService Unit Tests")
class UserReferenceServiceTest {

    @Mock
    private UserReferenceRepository userReferenceRepository;

    @InjectMocks
    private UserReferenceService userReferenceService;

    @Captor
    private ArgumentCaptor<UserReference> userReferenceCaptor;

    @Nested
    @DisplayName("Tests for ensureUserReferenceExists")
    class EnsureUserReferenceExistsTests {

        private final String keycloakUserId = "keycloak-123";

        @Test
        @DisplayName("Should save new user reference when it does not exist yet")
        void shouldSaveNewUserReferenceWhenNotExists() {
            // Given
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(false);

            // When
            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            // Then
            verify(userReferenceRepository).save(userReferenceCaptor.capture());
            UserReference savedRef = userReferenceCaptor.getValue();
            assertThat(savedRef.getKeycloakUserId()).isEqualTo(keycloakUserId);
        }

        @Test
        @DisplayName("Should do nothing when user reference already exists")
        void shouldDoNothingWhenUserReferenceAlreadyExists() {
            // Given
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(true);

            // When
            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            // Then
            verify(userReferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should catch DataIntegrityViolationException gracefully when concurrent creation happens")
        void shouldCatchDataIntegrityViolationOnConcurrentCreation() {
            // Given
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(false);
            when(userReferenceRepository.save(any(UserReference.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate key constraint"));

            // When / Then (Should not throw exception because of try-catch block)
            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            verify(userReferenceRepository).save(any(UserReference.class));
        }
    }

    @Nested
    @DisplayName("Tests for getDisplayId")
    class GetDisplayIdTests {

        private final String keycloakUserId = "keycloak-456";

        @Test
        @DisplayName("Should return formatted display ID when user reference exists")
        void shouldReturnFormattedDisplayIdWhenExists() {
            // Given
            UserReference reference = UserReference.builder()
                    .id(42L)
                    .keycloakUserId(keycloakUserId)
                    .build();

            when(userReferenceRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(reference));

            // When
            String displayId = userReferenceService.getDisplayId(keycloakUserId);

            // Then
            assertThat(displayId).isEqualTo("#42");
        }

        @Test
        @DisplayName("Should return fallback display ID when user reference does not exist")
        void shouldReturnFallbackDisplayIdWhenNotExists() {
            // Given
            when(userReferenceRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());

            // When
            String displayId = userReferenceService.getDisplayId(keycloakUserId);

            // Then
            assertThat(displayId).isEqualTo("#?????");
        }
    }
}