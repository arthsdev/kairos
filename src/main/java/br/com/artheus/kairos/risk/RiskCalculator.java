package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskCalculator {

    // External climate data is weighted higher than user reported occurrences
    private static final double CLIMATE_WEIGHT = 0.7;
    private static final double OCCURRENCE_WEIGHT = 0.3;

    private static final double SCORE_MIN = 1.0;
    private static final double SCORE_MAX = 4.0;

    // Smoothing factor to prevent a few occurrences from maxing out the score immediately
    private static final double OCCURRENCE_SMOOTHING_FACTOR = 5.0;

    public RiskLevel calculate(ClimateDataSummary climate, List<OccurrenceSummary> occurrences) {
        RiskLevel climateRisk = evaluateClimateRisk(climate);
        double climateScore = mapRiskLevelToScore(climateRisk);

        double totalOccurrenceScore = calculateTotalOccurrenceScore(occurrences);
        double normalizedOccurrenceScore = normalizeOccurrenceScore(totalOccurrenceScore);

        double finalScore = (climateScore * CLIMATE_WEIGHT) + (normalizedOccurrenceScore * OCCURRENCE_WEIGHT);

        return mapScoreToRiskLevel(finalScore);
    }

    private static RiskLevel evaluateClimateRisk(ClimateDataSummary climate) {
        if (climate.rainVolume() >= 50.0 || climate.windSpeed() >= 80.0) return RiskLevel.CRITICAL;
        if (climate.rainVolume() >= 21.0 || climate.windSpeed() >= 51.0) return RiskLevel.HIGH;
        if (climate.rainVolume() >= 5.0  || climate.windSpeed() >= 30.0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private double calculateTotalOccurrenceScore(List<OccurrenceSummary> occurrences) {
        double total = 0.0;
        for (OccurrenceSummary occurrence : occurrences) {
            double categoryWeight = switch (occurrence.category()) {
                case FLOOD, LANDSLIDE -> 3.0;
                case SEWAGE, MUDDY_WATER -> 2.0;
                case ILLEGAL_DUMPING, WILDFIRE -> 1.0;
                default -> 0.0;
            };

            double severityMultiplier = switch (occurrence.severity()) {
                case LOW -> 1.0;
                case MEDIUM -> 2.0;
                case HIGH -> 3.0;
                case CRITICAL -> 4.0;
            };

            total += categoryWeight * severityMultiplier;
        }
        return total;
    }

    private double normalizeOccurrenceScore(double totalScore) {
        // If there are no occurrences (totalScore == 0), return the minimum score (1.0)
        // If occurrences exist, add to the minimum value and cap at the maximum threshold (4.0)
        if (totalScore == 0.0) {
            return SCORE_MIN;
        }
        return Math.min(SCORE_MAX, SCORE_MIN + (totalScore / OCCURRENCE_SMOOTHING_FACTOR));
    }

    private double mapRiskLevelToScore(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1.0;
            case MEDIUM -> 2.0;
            case HIGH -> 3.0;
            case CRITICAL -> 4.0;
        };
    }

    private RiskLevel mapScoreToRiskLevel(double score) {
        if (score >= 3.5) return RiskLevel.CRITICAL;
        if (score >= 2.5) return RiskLevel.HIGH;
        if (score >= 1.5) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}