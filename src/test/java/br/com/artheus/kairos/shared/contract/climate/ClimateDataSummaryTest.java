package br.com.artheus.kairos.shared.contract.climate;

import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClimateDataSummaryTest {

    private static final LocalDateTime VALID_DATE = LocalDateTime.now();
    private static final RiskLevel VALID_RISK = RiskLevel.LOW;

    @Nested
    @DisplayName("Successful Instantiation Scenarios")
    class HappyPath {

        @Test
        @DisplayName("Should create ClimateDataSummary successfully when all fields are within physical boundaries")
        void shouldCreateSuccessfullyWithValidData() {
            ClimateDataSummary summary = new ClimateDataSummary(
                    25.0,
                    60.0,
                    10.0,
                    15.0,
                    VALID_RISK,
                    VALID_DATE
            );

            assertThat(summary).isNotNull();
            assertThat(summary.temperature()).isEqualTo(25.0);
            assertThat(summary.humidity()).isEqualTo(60.0);
            assertThat(summary.rainVolume()).isEqualTo(10.0);
            assertThat(summary.windSpeed()).isEqualTo(15.0);
            assertThat(summary.collectedAt()).isEqualTo(VALID_DATE);
        }

        @ParameterizedTest(name = "Temp={0}, Humidity={1}, Rain={2}, Wind={3}")
        @CsvSource({
                "-60.0, 0.0, 0.0, 0.0",      // Exact lower physical limits
                "60.0, 100.0, 500.0, 300.0"  // Exact upper physical limits
        })
        @DisplayName("Should allow instantiation exactly at the edge of physical boundaries")
        void shouldAllowInstantiationExactlyAtBoundaries(double temp, double humidity, double rain, double wind) {
            ClimateDataSummary summary = new ClimateDataSummary(temp, humidity, rain, wind, VALID_RISK, VALID_DATE);
            assertThat(summary).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation and Failure Scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("Should throw BusinessException when collectedAt is null")
        void shouldThrowExceptionWhenCollectedAtIsNull() {
            assertThatThrownBy(() -> new ClimateDataSummary(25.0, 60.0, 10.0, 15.0, VALID_RISK, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("collectedAt timestamp cannot be null");
        }

        @ParameterizedTest(name = "Invalid Temperature = {0}")
        @CsvSource({"-60.01", "60.01"})
        @DisplayName("Should throw BusinessException when temperature is out of physical bounds")
        void shouldThrowExceptionWhenTemperatureIsInvalid(double invalidTemp) {
            assertThatThrownBy(() -> new ClimateDataSummary(invalidTemp, 60.0, 10.0, 15.0, VALID_RISK, VALID_DATE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid temperature")
                    .hasMessageContaining("Must be between -60.00 and 60.00");
        }

        @ParameterizedTest(name = "Invalid Humidity = {0}")
        @CsvSource({"-0.01", "100.01"})
        @DisplayName("Should throw BusinessException when humidity is out of physical bounds")
        void shouldThrowExceptionWhenHumidityIsInvalid(double invalidHumidity) {
            assertThatThrownBy(() -> new ClimateDataSummary(25.0, invalidHumidity, 10.0, 15.0, VALID_RISK, VALID_DATE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid humidity")
                    .hasMessageContaining("Must be between 0.00 and 100.00");
        }

        @ParameterizedTest(name = "Invalid Rain Volume = {0}")
        @CsvSource({"-0.01", "500.01"})
        @DisplayName("Should throw BusinessException when rain volume is out of physical bounds")
        void shouldThrowExceptionWhenRainVolumeIsInvalid(double invalidRain) {
            assertThatThrownBy(() -> new ClimateDataSummary(25.0, 60.0, invalidRain, 15.0, VALID_RISK, VALID_DATE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid rain volume")
                    .hasMessageContaining("Must be between 0.00 and 500.00");
        }

        @ParameterizedTest(name = "Invalid Wind Speed = {0}")
        @CsvSource({"-0.01", "300.01"})
        @DisplayName("Should throw BusinessException when wind speed is out of physical bounds")
        void shouldThrowExceptionWhenWindSpeedIsInvalid(double invalidWind) {
            assertThatThrownBy(() -> new ClimateDataSummary(25.0, 60.0, 10.0, invalidWind, VALID_RISK, VALID_DATE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid wind speed")
                    .hasMessageContaining("Must be between 0.00 and 300.00");
        }
    }
}