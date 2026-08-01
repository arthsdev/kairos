package br.com.artheus.kairos.anonymization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserReference Entity Unit Tests")
class UserReferenceTest {

    @Test
    @DisplayName("Should set createdAt automatically when onCreate is called")
    void shouldSetCreatedAtOnPrePersist() {
        // Given
        UserReference reference = UserReference.builder()
                .id(1L)
                .keycloakUserId("keycloak-123")
                .build();

        assertThat(reference.getCreatedAt()).isNull();

        // When
        reference.onCreate();

        // Then
        assertThat(reference.getCreatedAt()).isNotNull();
        assertThat(reference.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should create UserReference correctly using AllArgsConstructor and Builder")
    void shouldCreateUserReferenceSuccessfully() {
        // Given
        Instant now = Instant.now();
        UserReference reference = new UserReference(10L, "keycloak-999", now);

        // Then
        assertThat(reference.getId()).isEqualTo(10L);
        assertThat(reference.getKeycloakUserId()).isEqualTo("keycloak-999");
        assertThat(reference.getCreatedAt()).isEqualTo(now);
    }
}