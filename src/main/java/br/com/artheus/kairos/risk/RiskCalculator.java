package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.climate.ClimateInput;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCalculator {

    private final RiskCalculatorProperties properties;

    // These values are constants because they represent strict algorithm limits
    private static final double SCORE_MIN = 1.0;
    private static final double SCORE_MAX = 4.0;

    public RiskLevel calculate(ClimateInput climate, List<OccurrenceSummary> occurrences) {

        if (climate == null) {
            log.error("Cannot calculate risk: ClimateInput is null");
            throw new BusinessException("Cannot calculate risk: ClimateInput is null");
        }

        if (occurrences == null) {
            log.error("Cannot calculate risk: occurrences list is null");
            throw new BusinessException("Cannot calculate risk: occurrences list is null");
        }

        RiskLevel climateRisk = evaluateClimateRisk(climate);
        double climateScore = mapRiskLevelToScore(climateRisk);

        double totalOccurrenceScore = calculateTotalOccurrenceScore(occurrences);
        double normalizedOccurrenceScore = normalizeOccurrenceScore(totalOccurrenceScore);

        double finalScore = (climateScore * properties.climateWeight()) + (normalizedOccurrenceScore * properties.occurrenceWeight());

        return mapScoreToRiskLevel(finalScore);
    }

    private RiskLevel evaluateClimateRisk(ClimateInput climate) {
        var rain = properties.thresholds().rain();
        var wind = properties.thresholds().wind();

        if (climate.rainVolume() >= rain.critical() || climate.windSpeed() >= wind.critical()) return RiskLevel.CRITICAL;
        if (climate.rainVolume() >= rain.high()     || climate.windSpeed() >= wind.high())     return RiskLevel.HIGH;
        if (climate.rainVolume() >= rain.medium()   || climate.windSpeed() >= wind.medium())   return RiskLevel.MEDIUM;
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
        return Math.min(SCORE_MAX, SCORE_MIN + (totalScore / properties.occurrenceSmoothingFactor()));
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