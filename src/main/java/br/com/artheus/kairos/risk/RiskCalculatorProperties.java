package br.com.artheus.kairos.risk;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kairos.risk-calculator")
public record RiskCalculatorProperties(
        @Positive double climateWeight,
        @Positive double occurrenceWeight,
        @Positive double occurrenceSmoothingFactor,
        @NotNull Thresholds thresholds
) {

    // Custom validation method to ensure weight consistency
    @AssertTrue(message = "The sum of climateWeight and occurrenceWeight must be exactly 1.0")
    public boolean isWeightsSumValid() {
        // Using a small delta tolerance to prevent double floating-point precision issues
        double delta = 0.0001;
        return Math.abs((climateWeight + occurrenceWeight) - 1.0) < delta;
    }

    // Main wrapper for system thresholds
    public record Thresholds(
            @NotNull RainThresholds rain,
            @NotNull WindThresholds wind
    ) {}

    // Severity thresholds for precipitation (Rain)
    public record RainThresholds(
            @Positive double medium,
            @Positive double high,
            @Positive double critical
    ) {}

    // Severity thresholds for velocity (Wind)
    public record WindThresholds(
            @Positive double medium,
            @Positive double high,
            @Positive double critical
    ) {}
}