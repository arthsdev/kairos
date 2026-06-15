package br.com.artheus.kairos.shared.contract.risk;

public record RiskMessage(
        String cityId,
        double temperature,
        int humidity,
        double rainVolume,
        double windSpeed
) {
}
