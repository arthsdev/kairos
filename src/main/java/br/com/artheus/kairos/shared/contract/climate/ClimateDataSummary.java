package br.com.artheus.kairos.shared.contract.climate;

import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Locale;

public record ClimateDataSummary(
        double temperature,
        double humidity,
        double rainVolume,
        double windSpeed,
        RiskLevel riskLevel,
        LocalDateTime collectedAt
) {
    // Physical limits defined to filter corrupted sensor data or API failures
    private static final double MIN_TEMPERATURE = -60.0;
    private static final double MAX_TEMPERATURE = 60.0; // extreme terrestrial temperature limits

    private static final double MIN_RAIN_VOLUME = 0.0;
    private static final double MAX_RAIN_VOLUME = 500.0; // extreme historic precipitation limit

    private static final double MIN_WIND_SPEED = 0.0;
    private static final double MAX_WIND_SPEED = 300.0; // extreme hurricane winds limit

    private static final double MIN_HUMIDITY = 0.0;
    private static final double MAX_HUMIDITY = 100.0; // strict physical limit for relative humidity

    // Compact Constructor - runs before automatic field assignment
    // Locale.US is explicitly set to avoid machine-specific decimal separator differences (e.g., "," vs ".")
    public ClimateDataSummary {
        // Enforced as a BusinessException to maintain API response consistency (422 instead of 500)
        if (collectedAt == null) {
            throw new BusinessException("collectedAt timestamp cannot be null");
        }

        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            throw new BusinessException(String.format(
                    Locale.US,
                    "Invalid temperature: %.2f°C. Must be between %.2f and %.2f",
                    temperature, MIN_TEMPERATURE, MAX_TEMPERATURE
            ));
        }

        if (rainVolume < MIN_RAIN_VOLUME || rainVolume > MAX_RAIN_VOLUME) {
            throw new BusinessException(String.format(
                    Locale.US,
                    "Invalid rain volume: %.2f mm. Must be between %.2f and %.2f",
                    rainVolume, MIN_RAIN_VOLUME, MAX_RAIN_VOLUME
            ));
        }

        if (windSpeed < MIN_WIND_SPEED || windSpeed > MAX_WIND_SPEED) {
            throw new BusinessException(String.format(
                    Locale.US,
                    "Invalid wind speed: %.2f km/h. Must be between %.2f and %.2f",
                    windSpeed, MIN_WIND_SPEED, MAX_WIND_SPEED
            ));
        }

        if (humidity < MIN_HUMIDITY || humidity > MAX_HUMIDITY) {
            throw new BusinessException(String.format(
                    Locale.US,
                    "Invalid humidity: %.2f%%. Must be between %.2f and %.2f",
                    humidity, MIN_HUMIDITY, MAX_HUMIDITY
            ));
        }
    }
}