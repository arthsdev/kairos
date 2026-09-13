package br.com.artheus.kairos.anonymization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserReferenceProvisioner Unit Tests")
class UserReferenceProvisionerTest {

    @Mock
    private UserReferenceRepository userReferenceRepository;

    @InjectMocks
    private UserReferenceProvisioner userReferenceProvisioner;

    @Test
    @DisplayName("Should build and save a new user reference for the given keycloak user ID")
    void shouldCreateAndPersistUserReference() {
        String keycloakUserId = "keycloak-123";

        userReferenceProvisioner.createUserReference(keycloakUserId);

        ArgumentCaptor<UserReference> captor = ArgumentCaptor.forClass(UserReference.class);
        verify(userReferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getKeycloakUserId()).isEqualTo(keycloakUserId);
    }

    @Test
    @DisplayName("Should propagate DataIntegrityViolationException when constraint is violated")
    void shouldPropagateExceptionOnConstraintViolation() {
        String keycloakUserId = "keycloak-concurrent";
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(userReferenceRepository).save(any(UserReference.class));

        assertThatThrownBy(() -> userReferenceProvisioner.createUserReference(keycloakUserId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}