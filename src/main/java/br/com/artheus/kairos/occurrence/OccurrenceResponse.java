package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;

import java.time.LocalDateTime;

public record OccurrenceResponse(

        String id,
        String title,
        String description,
        OccurrenceCategory category,
        OccurrenceSeverity severity,
        OccurrenceStatus status,
        Double latitude,
        Double longitude,
        String imageUrl,
        String userId,
        String reporterDisplayId,
        LocalDateTime createdAt,
        String cityId,
        OccurrenceActions actions
) {

    public static OccurrenceResponse from(Occurrence occurrence, OccurrenceActions actions) {
        return from(occurrence, actions, null);
    }

    public static OccurrenceResponse from(Occurrence occurrence, OccurrenceActions actions, String reporterDisplayId) {
        return new OccurrenceResponse(
                occurrence.getId(),
                occurrence.getTitle(),
                occurrence.getDescription(),
                occurrence.getCategory(),
                occurrence.getSeverity(),
                occurrence.getStatus(),
                occurrence.getLatitude(),
                occurrence.getLongitude(),
                occurrence.getImageUrl(),
                occurrence.getUserId(),
                reporterDisplayId,
                occurrence.getCreatedAt(),
                occurrence.getCityId(),
                actions
        );
    }
}