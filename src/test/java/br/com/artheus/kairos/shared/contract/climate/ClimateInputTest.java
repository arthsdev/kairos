package br.com.artheus.kairos.shared.contract.climate;

import br.com.artheus.kairos.shared.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClimateInput Record Unit Tests")
class ClimateInputTest {

    @Nested
    @DisplayName("Tests for static factory method from()")
    class FromFactoryTests {

        @Test
        @DisplayName("Should correctly map all fields from ClimateDataSummary to ClimateInput")
        void shouldMapAllFieldsCorrectly() {
            // Given
            ClimateDataSummary summary = new ClimateDataSummary(
                    25.5,
                    80.0,
                    12.4,
                    15.2,
                    RiskLevel.LOW,
                    LocalDateTime.now()
            );

            // When
            ClimateInput result = ClimateInput.from(summary);

            // Then
            assertThat(result.temperature()).isEqualTo(25.5);
            assertThat(result.humidity()).isEqualTo(80.0);
            assertThat(result.rainVolume()).isEqualTo(12.4);
            assertThat(result.windSpeed()).isEqualTo(15.2);
        }
    }
}