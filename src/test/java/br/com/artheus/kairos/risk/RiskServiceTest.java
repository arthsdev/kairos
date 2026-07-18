package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateInput;
import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceDataProvider;
import br.com.artheus.kairos.shared.contract.occurrence.OccurrenceSummary;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.contract.risk.RiskProvider;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskService Unit Tests")
class RiskServiceTest {

    @Mock
    private OccurrenceDataProvider occurrenceDataProvider;

    @Mock
    private RiskCalculator riskCalculator;

    @Mock
    private RiskProvider riskProvider;

    @Mock
    private CityProvider cityProvider;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private RiskService riskService;

    @Nested
    @DisplayName("Tests for processRiskCalculation")
    class ProcessRiskCalculationTests {

        private final String cityId = "city-42";
        private final String cityName = "Blumenau";

        private final RiskMessage mockMessage = new RiskMessage(
                cityId, 28.5, 82, 15.0, 45.2
        );

        private final List<OccurrenceSummary> mockOccurrences = Collections.emptyList();

        @Test
        @DisplayName("Should save risk and dispatch alert in strict order when risk level is HIGH")
        void shouldSaveRiskAndNotifyInOrderWhenRiskIsHigh() {
            // Given
            when(occurrenceDataProvider.findVerifiedByCityId(cityId)).thenReturn(mockOccurrences);
            when(riskCalculator.calculate(any(ClimateInput.class), eq(mockOccurrences))).thenReturn(RiskLevel.HIGH);
            when(cityProvider.findCityById(cityId)).thenReturn(new CityLocation(cityId, cityName, -26.9, -49.0));

            // When
            riskService.processRiskCalculation(mockMessage);

            // Then
            InOrder inOrder = inOrder(riskProvider, cityProvider, notificationSender);

            inOrder.verify(riskProvider).saveCalculatedRisk(cityId, RiskLevel.HIGH);
            inOrder.verify(cityProvider).findCityById(cityId);
            inOrder.verify(notificationSender).sendNotification(cityName, RiskLevel.HIGH);
        }

        @Test
        @DisplayName("Should save risk and dispatch alert in strict order when risk level is CRITICAL")
        void shouldSaveRiskAndNotifyInOrderWhenRiskIsCritical() {
            // Given
            when(occurrenceDataProvider.findVerifiedByCityId(cityId)).thenReturn(mockOccurrences);
            when(riskCalculator.calculate(any(ClimateInput.class), eq(mockOccurrences))).thenReturn(RiskLevel.CRITICAL);
            when(cityProvider.findCityById(cityId)).thenReturn(new CityLocation(cityId, cityName, -26.9, -49.0));

            // When
            riskService.processRiskCalculation(mockMessage);

            // Then
            InOrder inOrder = inOrder(riskProvider, cityProvider, notificationSender);

            inOrder.verify(riskProvider).saveCalculatedRisk(cityId, RiskLevel.CRITICAL);
            inOrder.verify(cityProvider).findCityById(cityId);
            inOrder.verify(notificationSender).sendNotification(cityName, RiskLevel.CRITICAL);
        }

        @Test
        @DisplayName("Should save risk but NEVER trigger notification pipeline when risk level is MEDIUM")
        void shouldSaveRiskAndSkipNotificationWhenRiskIsMedium() {
            // Given
            when(occurrenceDataProvider.findVerifiedByCityId(cityId)).thenReturn(mockOccurrences);
            when(riskCalculator.calculate(any(ClimateInput.class), eq(mockOccurrences))).thenReturn(RiskLevel.MEDIUM);

            // When
            riskService.processRiskCalculation(mockMessage);

            // Then
            verify(riskProvider).saveCalculatedRisk(cityId, RiskLevel.MEDIUM);

            verify(cityProvider, never()).findCityById(any());
            verify(notificationSender, never()).sendNotification(any(), any());
        }

        @Test
        @DisplayName("Should save risk but NEVER trigger notification pipeline when risk level is LOW")
        void shouldSaveRiskAndSkipNotificationWhenRiskIsLow() {
            // Given
            when(occurrenceDataProvider.findVerifiedByCityId(cityId)).thenReturn(mockOccurrences);
            when(riskCalculator.calculate(any(ClimateInput.class), eq(mockOccurrences))).thenReturn(RiskLevel.LOW);

            // When
            riskService.processRiskCalculation(mockMessage);

            // Then
            verify(riskProvider).saveCalculatedRisk(cityId, RiskLevel.LOW);

            verify(cityProvider, never()).findCityById(any());
            verify(notificationSender, never()).sendNotification(any(), any());
        }
    }
}