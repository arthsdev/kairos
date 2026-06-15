package br.com.artheus.kairos.shared.contract.risk;

public record CalculatedRiskMessage(
        String cityId,
        double temperature,
        double humidity,
        double rainVolume,
        double windSpeed
) {
}
