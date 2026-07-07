package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceDataProvider;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.security.SecurityService;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.exception.ForbiddenException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.shared.pagination.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OccurrenceService implements OccurrenceDataProvider {

    private final OccurrenceRepository occurrenceRepository;
    private final SecurityService securityService;

    @Transactional
    public OccurrenceResponse createOccurrence(OccurrenceRequest request, String userId) {

        Occurrence occurrence = Occurrence.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .severity(request.severity())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .imageUrl(request.imageUrl())
                .userId(userId)
                .cityId(request.cityId())
                .build();

        occurrenceRepository.save(occurrence);

        return OccurrenceResponse.from(occurrence);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OccurrenceResponse> getVerifiedOccurrences(Pageable pageable) {
        Page<OccurrenceResponse> page = occurrenceRepository
                .findAllByStatus(OccurrenceStatus.VERIFIED, pageable)
                .map(OccurrenceResponse::from);

        return new PaginatedResponse<>(page);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OccurrenceResponse> getMyOccurrences(String userId, OccurrenceStatus status, Pageable pageable) {
        Page<Occurrence> occurrencePage;

        if (status != null) {
            occurrencePage = occurrenceRepository.findAllByUserIdAndStatus(userId, status, pageable);
        } else {
            occurrencePage = occurrenceRepository.findAllByUserId(userId, pageable);
        }

        // 1. Maps the entity page to a DTO page
        Page<OccurrenceResponse> responsePage = occurrencePage.map(OccurrenceResponse::from);

        // 2. Wraps the DTO page into the custom PaginatedResponse
        return new PaginatedResponse<>(responsePage);
    }

    @Transactional(readOnly = true)
    public List<OccurrenceSummary> findVerifiedByCityId(String cityId) {
        List<Occurrence> verifiedCurrencies = occurrenceRepository.findByCityIdAndStatusIn(
                cityId,
                List.of(OccurrenceStatus.VERIFIED, OccurrenceStatus.RESOLVED)
        );

        return verifiedCurrencies.stream()
                .map(occurrence -> new OccurrenceSummary(
                        occurrence.getCategory(),
                        occurrence.getSeverity(),
                        occurrence.getStatus(),
                        cityId
                )).toList();
    }

    @Transactional
    public OccurrenceResponse updateOccurrence(String id, OccurrenceRequest request) {
        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

        String currentUserId = securityService.getCurrentUserId();
        boolean isAdmin = securityService.isAdmin();

        if (!occurrence.getUserId().equals(currentUserId) && !isAdmin) {
            throw new ForbiddenException("You can't update the occurrence");
        }

        if (request.title() != null) occurrence.changeTitle(request.title());
        if (request.description() != null) occurrence.changeDescription(request.description());

        occurrenceRepository.save(occurrence);

        return OccurrenceResponse.from(occurrence);
    }

    @Transactional
    public OccurrenceResponse deleteOccurrence(String id) {

        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

        String currentUserId = securityService.getCurrentUserId();
        boolean isAdmin = securityService.isAdmin();

        if (!occurrence.getUserId().equals(currentUserId) && !isAdmin) {
            throw new ForbiddenException("You can't delete the occurrence");
        }

        occurrence.delete();
        occurrenceRepository.save(occurrence);

        return OccurrenceResponse.from(occurrence);
    }
}

