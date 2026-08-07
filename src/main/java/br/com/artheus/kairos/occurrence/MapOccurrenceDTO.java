package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;

public record MapOccurrenceDTO(
        String id,
        Double latitude,
        Double longitude,
        OccurrenceCategory category,
        OccurrenceSeverity severity,
        OccurrenceStatus status
) {
}
