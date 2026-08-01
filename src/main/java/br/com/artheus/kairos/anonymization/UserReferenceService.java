package br.com.artheus.kairos.anonymization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserReferenceService {

    private final UserReferenceRepository userReferenceRepository;

    @Transactional
    public void ensureUserReferenceExists(String keycloakUserId) {
        if (!userReferenceRepository.existsByKeycloakUserId(keycloakUserId)) {
            try {
                UserReference newRef = UserReference.builder()
                        .keycloakUserId(keycloakUserId)
                        .build();
                userReferenceRepository.save(newRef);
            } catch (DataIntegrityViolationException e) {
                log.debug("UserReference for {} already created by a concurrent request, skipping", keycloakUserId);
            }
        }
    }

    public String getDisplayId(String keycloakUserId) {
        return userReferenceRepository.findByKeycloakUserId(keycloakUserId)
                .map(ref -> "#" + ref.getId())
                .orElse("#?????");
    }
}