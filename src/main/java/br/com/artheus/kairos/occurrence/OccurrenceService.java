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
    private final OccurrenceActionsCalculator occurrenceActionsCalculator;

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

        boolean isAdmin = securityService.isAdmin();
        OccurrenceActions actions = occurrenceActionsCalculator.calculate(occurrence, userId, isAdmin);

        return OccurrenceResponse.from(occurrence, actions);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OccurrenceResponse> getVerifiedOccurrences(Pageable pageable) {

        String currentUserId = securityService.getCurrentUserId();
        boolean isAdmin = securityService.isAdmin();

        Page<OccurrenceResponse> page = occurrenceRepository
                .findAllByStatusAndDeletedAtIsNull(OccurrenceStatus.VERIFIED, pageable)
                .map(occurrence ->
                        OccurrenceResponse.from(
                                occurrence,
                                occurrenceActionsCalculator.calculate(occurrence, currentUserId, isAdmin)
                        ));

        return new PaginatedResponse<>(page);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OccurrenceResponse> getMyOccurrences(String userId, OccurrenceStatus status, Pageable pageable) {
        Page<Occurrence> occurrencePage;

        if (status != null) {
            occurrencePage = occurrenceRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(userId, status, pageable);
        } else {
            occurrencePage = occurrenceRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
        }

        boolean isAdmin = securityService.isAdmin();

        // Maps the entity page to DTOs using lambda with the calculator
        Page<OccurrenceResponse> responsePage = occurrencePage.map(occurrence ->
                OccurrenceResponse.from(
                        occurrence,
                        occurrenceActionsCalculator.calculate(occurrence, userId, isAdmin)
                )
        );

        return new PaginatedResponse<>(responsePage);
    }

    @Transactional(readOnly = true)
    public List<OccurrenceSummary> findVerifiedByCityId(String cityId) {
        List<Occurrence> verifiedCurrencies = occurrenceRepository.findByCityIdAndStatusInAndDeletedAtIsNull(
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
    public OccurrenceResponse updateOccurrence(String id, UpdateOccurrenceRequest request) {
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

        OccurrenceActions actions = occurrenceActionsCalculator.calculate(occurrence, currentUserId, isAdmin);

        return OccurrenceResponse.from(occurrence, actions);
    }

    @Transactional
    public OccurrenceResponse verifyOccurrence(String id) {
        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

        String currentUserId = securityService.getCurrentUserId();
        boolean isAdmin = securityService.isAdmin();

        if (!isAdmin) {
            throw new ForbiddenException("Only admins can verify occurrences");
        }

        occurrence.verify();
        occurrenceRepository.save(occurrence);

        OccurrenceActions actions = occurrenceActionsCalculator.calculate(occurrence, currentUserId, isAdmin);

        return OccurrenceResponse.from(occurrence, actions);
    }

    @Transactional
    public OccurrenceResponse resolveOccurrence(String id) {
        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

        String currentUserId = securityService.getCurrentUserId();
        boolean isAdmin = securityService.isAdmin();

        if (!isAdmin) {
            throw new ForbiddenException("Only admins can resolve occurrences");
        }

        occurrence.resolve();
        occurrenceRepository.save(occurrence);

        OccurrenceActions actions = occurrenceActionsCalculator.calculate(occurrence, currentUserId, isAdmin);

        return OccurrenceResponse.from(occurrence, actions);
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

        OccurrenceActions actions = occurrenceActionsCalculator.calculate(occurrence, currentUserId, isAdmin);

        return OccurrenceResponse.from(occurrence, actions);
    }
}

