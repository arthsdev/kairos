package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_UPPER=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",

        "spring.flyway.enabled=false",

        "spring.jpa.hibernate.ddl-auto=create-drop",
})
class OccurrenceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OccurrenceRepository occurrenceRepository;

    @Test
    @DisplayName("Should find only active occurrences with allowed statuses within the specified city")
    void shouldFindOnlyActiveOccurrencesWithSpecifiedStatusesAndCity() {
        String targetCityId = "city-abc-123";
        String userId = "user-xyz-999";
        LocalDateTime now = LocalDateTime.now();

        Occurrence activeVerified = Occurrence.builder()
                .title("Active Verified Incident with more than 20 characters")
                .category(OccurrenceCategory.FLOOD)
                .severity(OccurrenceSeverity.HIGH)
                .status(OccurrenceStatus.VERIFIED)
                .latitude(-23.55052)
                .longitude(-46.633308)
                .userId(userId)
                .cityId(targetCityId)
                .createdAt(now)
                .build();

        Occurrence deletedVerified = Occurrence.builder()
                .title("Soft-deleted Verified Incident with more than 20 characters")
                .category(OccurrenceCategory.LANDSLIDE)
                .severity(OccurrenceSeverity.CRITICAL)
                .status(OccurrenceStatus.VERIFIED)
                .latitude(-23.55052)
                .longitude(-46.633308)
                .userId(userId)
                .cityId(targetCityId)
                .createdAt(now)
                .deletedAt(now)
                .build();

        Occurrence activePending = Occurrence.builder()
                .title("Active Pending Incident with more than 20 characters")
                .category(OccurrenceCategory.SEWAGE)
                .severity(OccurrenceSeverity.LOW)
                .status(OccurrenceStatus.PENDING)
                .latitude(-23.55052)
                .longitude(-46.633308)
                .userId(userId)
                .cityId(targetCityId)
                .createdAt(now)
                .build();

        Occurrence persistedActive = entityManager.persistAndFlush(activeVerified);
        entityManager.persistAndFlush(deletedVerified);
        entityManager.persistAndFlush(activePending);

        entityManager.clear();

        // Act
        List<OccurrenceStatus> targetStatuses = List.of(OccurrenceStatus.VERIFIED, OccurrenceStatus.RESOLVED);
        List<Occurrence> result = occurrenceRepository.findByCityIdAndStatusInAndDeletedAtIsNull(
                targetCityId,
                targetStatuses
        );

        // Assert
        assertThat(result)
                .as("Should contain exactly 1 occurrence that is both active and verified")
                .hasSize(1);

        assertThat(result.get(0).getId())
                .as("The returned occurrence must be the active verified one")
                .isEqualTo(persistedActive.getId());
    }
}