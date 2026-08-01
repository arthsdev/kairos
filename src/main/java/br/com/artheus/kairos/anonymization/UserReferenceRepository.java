package br.com.artheus.kairos.anonymization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserReferenceRepository extends JpaRepository<UserReference, Long> {

    Optional<UserReference> findByKeycloakUserId(String keycloakUserId);

    boolean existsByKeycloakUserId(String keycloakUserId);
}