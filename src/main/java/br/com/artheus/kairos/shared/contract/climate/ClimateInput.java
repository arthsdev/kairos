package br.com.artheus.kairos.shared.contract.climate;

public record ClimateInput(
        double temperature,
        double humidity,
        double rainVolume,
        double windSpeed
) {
    public static ClimateInput from(ClimateDataSummary summary) {
        return new ClimateInput(
                summary.temperature(),
                summary.humidity(),
                summary.rainVolume(),
                summary.windSpeed()
        );
    }
}
