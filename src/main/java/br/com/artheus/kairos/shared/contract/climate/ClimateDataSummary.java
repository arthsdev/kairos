package br.com.artheus.kairos.shared.contract.climate;

import br.com.artheus.kairos.shared.enums.RiskLevel;

import java.time.LocalDateTime;

public record ClimateDataSummary(
        double temperature,
        double humidity,
        double rainVolume,
        double windSpeed,
        RiskLevel riskLevel,
        LocalDateTime collectedAt
) {
}
