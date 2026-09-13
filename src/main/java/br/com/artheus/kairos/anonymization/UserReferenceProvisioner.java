package br.com.artheus.kairos.anonymization;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserReferenceProvisioner {

    private final UserReferenceRepository userReferenceRepository;

    public UserReferenceProvisioner(UserReferenceRepository userReferenceRepository) {
        this.userReferenceRepository = userReferenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUserReference(String keycloakUserId) {
        UserReference newRef = UserReference.builder()
                .keycloakUserId(keycloakUserId)
                .build();
        userReferenceRepository.save(newRef);
    }
}