package br.com.artheus.kairos.shared.contract.risk;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;

public record OccurrenceSummary(
        OccurrenceCategory category,
        OccurrenceSeverity severity,
        OccurrenceStatus status,
        String cityId
) {
}
