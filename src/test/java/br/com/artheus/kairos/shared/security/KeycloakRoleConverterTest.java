package br.com.artheus.kairos.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private KeycloakRoleConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakRoleConverter();
    }

    @Test
    @DisplayName("Happy path: Should convert roles adding the ROLE_ prefix when necessary")
    void shouldConvertRolesAddingRolePrefix() {
        // Arrange
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin", "user"))
        ));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .hasSize(2)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    @DisplayName("Avoid duplication: Should not add ROLE_ if the role already has the prefix")
    void shouldNotDuplicateRolePrefixWhenAlreadyPresent() {
        // Arrange
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("ROLE_ADMIN", "operator"))
        ));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_operator");
    }

    @Test
    @DisplayName("Missing realm_access: Should return an empty collection")
    void shouldReturnEmptyListWhenRealmAccessIsMissing() {
        // Arrange
        Jwt jwt = createJwt(Map.of("sub", "123456"));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Missing 'roles' key in realm_access: Should return an empty collection")
    void shouldReturnEmptyListWhenRolesKeyIsMissingInRealmAccess() {
        // Arrange
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("other_claim", "value")
        ));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Invalid 'roles' type: Should return an empty collection when value is not a List")
    void shouldReturnEmptyListWhenRolesIsNotAList() {
        // Arrange
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", "invalid_string_instead_of_list")
        ));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Invalid elements: Should filter out non-String elements from the roles list")
    void shouldFilterOutNonStringElementsInRolesList() {
        // Arrange
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin", 123, true, "manager"))
        ));

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .hasSize(2)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_manager");
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}