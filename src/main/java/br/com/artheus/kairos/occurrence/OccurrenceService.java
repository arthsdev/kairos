package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OccurrenceService {

    private final OccurrenceRepository occurrenceRepository;

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
    public List<OccurrenceResponse> getVerifiedOccurrences() {
        return occurrenceRepository.findAllByStatus(OccurrenceStatus.VERIFIED)
                .stream()
                .map(OccurrenceResponse::from)
                .toList();
    }

    @Transactional
    public OccurrenceResponse updateOccurrence(String id, OccurrenceRequest request, String userId) {

        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

        if (!occurrence.getUserId().equals(userId)) {
            throw new BusinessException("You can't update the occurrence");
        }

        if (request.title() != null) occurrence.changeTitle(request.title());
        if (request.description() != null) occurrence.changeDescription(request.description());

        occurrenceRepository.save(occurrence);
        return OccurrenceResponse.from(occurrence);
    }

    @Transactional
    public OccurrenceResponse deleteOccurrence(String id, String userId) {

        Occurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));

            if (!occurrence.getUserId().equals(userId)) {
                throw new BusinessException("You can't delete the occurrence");
            }

        if (occurrence.getDeletedAt() != null) {
            throw new BusinessException("Occurrence already deleted");
        }

        occurrence.delete();
        occurrenceRepository.save(occurrence);

        return OccurrenceResponse.from(occurrence);

        }
    }

