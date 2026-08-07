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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
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

        assertThat(result.getFirst().getId())
                .as("The returned occurrence must be the active verified one")
                .isEqualTo(persistedActive.getId());
    }

    @Test
    @DisplayName("Should project map DTOs for VERIFIED and RESOLVED occurrences when non-admin statuses are requested")
    void shouldFindMapDataForNonAdminStatusesExcludingSoftDeleted() {
        String cityId = "city-abc-123";
        String userId = "user-xyz-999";
        LocalDateTime now = LocalDateTime.now();

        Occurrence activeVerified = Occurrence.builder()
                .title("Active Verified Incident for Map View")
                .category(OccurrenceCategory.FLOOD)
                .severity(OccurrenceSeverity.MEDIUM)
                .status(OccurrenceStatus.VERIFIED)
                .latitude(-23.55100)
                .longitude(-46.63400)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .build();

        Occurrence activeResolved = Occurrence.builder()
                .title("Active Resolved Incident for Map View")
                .category(OccurrenceCategory.FLOOD)
                .severity(OccurrenceSeverity.LOW)
                .status(OccurrenceStatus.RESOLVED)
                .latitude(-23.55300)
                .longitude(-46.63600)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .build();

        Occurrence deletedVerified = Occurrence.builder()
                .title("Soft-deleted Verified Incident for Map View")
                .category(OccurrenceCategory.WILDFIRE)
                .severity(OccurrenceSeverity.CRITICAL)
                .status(OccurrenceStatus.VERIFIED)
                .latitude(-23.55200)
                .longitude(-46.63500)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .deletedAt(now)
                .build();

        Occurrence persistedVerified = entityManager.persistAndFlush(activeVerified);
        Occurrence persistedResolved = entityManager.persistAndFlush(activeResolved);
        entityManager.persistAndFlush(deletedVerified);

        entityManager.clear();

        List<OccurrenceStatus> nonAdminStatuses = List.of(OccurrenceStatus.VERIFIED, OccurrenceStatus.RESOLVED);
        List<MapOccurrenceDTO> results = occurrenceRepository.findMapDataByStatusIn(nonAdminStatuses);

        assertThat(results)
                .as("Should return active VERIFIED and RESOLVED occurrences, excluding soft-deleted ones")
                .hasSize(2);

        assertThat(results)
                .extracting(MapOccurrenceDTO::id)
                .containsExactlyInAnyOrder(persistedVerified.getId(), persistedResolved.getId());

        MapOccurrenceDTO verifiedDto = results.stream()
                .filter(dto -> dto.id().equals(persistedVerified.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(verifiedDto.latitude()).isEqualTo(-23.55100);
        assertThat(verifiedDto.longitude()).isEqualTo(-46.63400);
        assertThat(verifiedDto.category()).isEqualTo(OccurrenceCategory.FLOOD);
        assertThat(verifiedDto.severity()).isEqualTo(OccurrenceSeverity.MEDIUM);
        assertThat(verifiedDto.status()).isEqualTo(OccurrenceStatus.VERIFIED);
    }

    @Test
    @DisplayName("Should project map DTOs for PENDING, VERIFIED, and RESOLVED occurrences when admin statuses are requested")
    void shouldFindMapDataForAdminStatusesExcludingSoftDeleted() {
        String cityId = "city-abc-123";
        String userId = "user-xyz-999";
        LocalDateTime now = LocalDateTime.now();

        Occurrence activePending = Occurrence.builder()
                .title("Active Pending Incident for Map View")
                .category(OccurrenceCategory.LANDSLIDE)
                .severity(OccurrenceSeverity.HIGH)
                .status(OccurrenceStatus.PENDING)
                .latitude(-23.55052)
                .longitude(-46.633308)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .build();

        Occurrence activeVerified = Occurrence.builder()
                .title("Active Verified Incident for Map View")
                .category(OccurrenceCategory.FLOOD)
                .severity(OccurrenceSeverity.MEDIUM)
                .status(OccurrenceStatus.VERIFIED)
                .latitude(-23.55100)
                .longitude(-46.63400)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .build();

        Occurrence deletedPending = Occurrence.builder()
                .title("Soft-deleted Pending Incident for Map View")
                .category(OccurrenceCategory.SEWAGE)
                .severity(OccurrenceSeverity.MEDIUM)
                .status(OccurrenceStatus.PENDING)
                .latitude(-23.55400)
                .longitude(-46.63700)
                .userId(userId)
                .cityId(cityId)
                .createdAt(now)
                .deletedAt(now)
                .build();

        Occurrence persistedPending = entityManager.persistAndFlush(activePending);
        Occurrence persistedVerified = entityManager.persistAndFlush(activeVerified);
        entityManager.persistAndFlush(deletedPending);

        entityManager.clear();

        List<OccurrenceStatus> adminStatuses = List.of(OccurrenceStatus.PENDING, OccurrenceStatus.VERIFIED, OccurrenceStatus.RESOLVED);
        List<MapOccurrenceDTO> results = occurrenceRepository.findMapDataByStatusIn(adminStatuses);

        assertThat(results)
                .as("Should return active PENDING and VERIFIED occurrences, excluding soft-deleted ones")
                .hasSize(2);

        assertThat(results)
                .extracting(MapOccurrenceDTO::id)
                .containsExactlyInAnyOrder(persistedPending.getId(), persistedVerified.getId());
    }
}