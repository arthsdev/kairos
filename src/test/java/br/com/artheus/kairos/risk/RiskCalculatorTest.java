package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.climate.ClimateInput;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskCalculatorTest {

    private RiskCalculator riskCalculator;
    private RiskCalculatorProperties properties;

    // Dummy values to satisfy record constructors without affecting risk calculation rules
    private static final double DUMMY_TEMP = 25.0;
    private static final double DUMMY_HUMIDITY = 70.0;
    private static final OccurrenceStatus DUMMY_STATUS = OccurrenceStatus.PENDING;
    private static final String DUMMY_CITY_ID = "city-123";

    @BeforeEach
    void setUp() {
        // Symmetric weights (0.5 / 0.5) are used to make the final score arithmetic predictable and easy to assert
        properties = new RiskCalculatorProperties(
                0.5,
                0.5,
                10.0,
                new RiskCalculatorProperties.Thresholds(
                        new RiskCalculatorProperties.RainThresholds(10.0, 50.0, 100.0),
                        new RiskCalculatorProperties.WindThresholds(15.0, 40.0, 80.0)
                )
        );

        riskCalculator = new RiskCalculator(properties);
    }

    @Nested
    @DisplayName("Climate Boundary Scenarios (Rain vs Wind)")
    class ClimateBoundaries {

        @ParameterizedTest(name = "Rain={0}mm, Wind={1}km/h -> Expected Climate Risk={2}")
        @MethodSource("provideClimateBoundaryCases")
        @DisplayName("Should evaluate climate risk at boundaries prioritizing the highest risk (OR condition)")
        void shouldEvaluateClimateRiskAtBoundaries(double rain, double wind, RiskLevel expectedClimateRisk) {
            ClimateInput input = new ClimateInput(DUMMY_TEMP, DUMMY_HUMIDITY, rain, wind);

            RiskLevel finalRisk = riskCalculator.calculate(input, Collections.emptyList());

            // Reverse-map the expected climate risk to the expected final level based on 0.5/0.5 weights
            RiskLevel expectedFinalLevel = switch (expectedClimateRisk) {
                case LOW -> RiskLevel.LOW;
                case MEDIUM, HIGH -> RiskLevel.MEDIUM;
                case CRITICAL -> RiskLevel.HIGH;
            };

            assertThat(finalRisk).isEqualTo(expectedFinalLevel);
        }

        private static Stream<Arguments> provideClimateBoundaryCases() {
            return Stream.of(
                    // --- LOW Boundaries ---
                    Arguments.of(0.0, 0.0, RiskLevel.LOW),
                    Arguments.of(9.99, 14.99, RiskLevel.LOW),

                    // --- MEDIUM Boundaries ---
                    Arguments.of(10.0, 0.0, RiskLevel.MEDIUM),
                    Arguments.of(0.0, 15.0, RiskLevel.MEDIUM),
                    Arguments.of(49.99, 39.99, RiskLevel.MEDIUM),

                    // --- HIGH Boundaries ---
                    Arguments.of(50.0, 0.0, RiskLevel.HIGH),
                    Arguments.of(0.0, 40.0, RiskLevel.HIGH),
                    Arguments.of(99.99, 79.99, RiskLevel.HIGH),

                    // --- CRITICAL Boundaries ---
                    Arguments.of(100.0, 0.0, RiskLevel.CRITICAL),
                    Arguments.of(0.0, 80.0, RiskLevel.CRITICAL),
                    Arguments.of(120.0, 90.0, RiskLevel.CRITICAL)
            );
        }
    }

    @Nested
    @DisplayName("Occurrence Scoring and Normalization")
    class OccurrenceCalculations {

        @Test
        @DisplayName("Should calculate weighted occurrence score based on category and severity")
        void shouldCalculateWeightedOccurrenceScore() {
            List<OccurrenceSummary> occurrences = List.of(
                    new OccurrenceSummary(OccurrenceCategory.FLOOD, OccurrenceSeverity.HIGH, DUMMY_STATUS, DUMMY_CITY_ID),
                    new OccurrenceSummary(OccurrenceCategory.ILLEGAL_DUMPING, OccurrenceSeverity.LOW, DUMMY_STATUS, DUMMY_CITY_ID)
            );
            ClimateInput climate = new ClimateInput(DUMMY_TEMP, DUMMY_HUMIDITY, 0.0, 0.0);

            RiskLevel finalRisk = riskCalculator.calculate(climate, occurrences);

            assertThat(finalRisk).isEqualTo(RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("Should cap normalized occurrence score at maximum value (4.0)")
        void shouldCapNormalizedOccurrenceScoreAtMax() {
            List<OccurrenceSummary> occurrences = List.of(
                    new OccurrenceSummary(OccurrenceCategory.LANDSLIDE, OccurrenceSeverity.CRITICAL, DUMMY_STATUS, DUMMY_CITY_ID),
                    new OccurrenceSummary(OccurrenceCategory.FLOOD, OccurrenceSeverity.CRITICAL, DUMMY_STATUS, DUMMY_CITY_ID),
                    new OccurrenceSummary(OccurrenceCategory.FLOOD, OccurrenceSeverity.CRITICAL, DUMMY_STATUS, DUMMY_CITY_ID)
            );
            ClimateInput climate = new ClimateInput(DUMMY_TEMP, DUMMY_HUMIDITY, 0.0, 0.0);

            RiskLevel finalRisk = riskCalculator.calculate(climate, occurrences);

            assertThat(finalRisk).isEqualTo(RiskLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("Validation and Edge Cases")
    class Resilience {

        @Test
        @DisplayName("Should throw BusinessException when ClimateInput is null")
        void shouldThrowExceptionWhenClimateInputIsNull() {
            List<OccurrenceSummary> occurrences = Collections.emptyList();

            assertThatThrownBy(() -> riskCalculator.calculate(null, occurrences))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot calculate risk: ClimateInput is null");
        }

        @Test
        @DisplayName("Should throw BusinessException when occurrences list is null")
        void shouldThrowExceptionWhenOccurrencesListIsNull() {
            ClimateInput climate = new ClimateInput(DUMMY_TEMP, DUMMY_HUMIDITY, 10.0, 15.0);

            assertThatThrownBy(() -> riskCalculator.calculate(climate, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot calculate risk: occurrences list is null");
        }

        @Test
        @DisplayName("Should calculate risk level successfully when occurrences list is empty")
        void shouldCalculateSuccessfullyWithEmptyOccurrences() {
            ClimateInput climate = new ClimateInput(DUMMY_TEMP, DUMMY_HUMIDITY, 10.0, 0.0);

            RiskLevel result = riskCalculator.calculate(climate, Collections.emptyList());

            assertThat(result).isEqualTo(RiskLevel.MEDIUM);
        }
    }
}