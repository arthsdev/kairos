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

    @Mock
    private UserReferenceProvisioner userReferenceProvisioner;

    @InjectMocks
    private UserReferenceService userReferenceService;

    @Nested
    @DisplayName("Tests for ensureUserReferenceExists")
    class EnsureUserReferenceExistsTests {

        private final String keycloakUserId = "keycloak-123";

        @Test
        @DisplayName("Should create user reference via provisioner when it does not exist yet")
        void shouldCreateUserReferenceWhenNotExists() {
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(false);

            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            verify(userReferenceProvisioner, times(1)).createUserReference(keycloakUserId);
        }

        @Test
        @DisplayName("Should do nothing when user reference already exists")
        void shouldDoNothingWhenUserReferenceAlreadyExists() {
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(true);

            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            verify(userReferenceProvisioner, never()).createUserReference(any());
        }

        @Test
        @DisplayName("Should catch DataIntegrityViolationException gracefully when concurrent creation happens")
        void shouldCatchDataIntegrityViolationOnConcurrentCreation() {
            when(userReferenceRepository.existsByKeycloakUserId(keycloakUserId)).thenReturn(false);
            doThrow(new DataIntegrityViolationException("Duplicate key constraint"))
                    .when(userReferenceProvisioner).createUserReference(keycloakUserId);

            userReferenceService.ensureUserReferenceExists(keycloakUserId);

            verify(userReferenceProvisioner, times(1)).createUserReference(keycloakUserId);
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