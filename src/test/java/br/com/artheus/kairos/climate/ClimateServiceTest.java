package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.cities.City;
import br.com.artheus.kairos.cities.CityRepository;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import br.com.artheus.kairos.shared.contract.risk.RiskCalculationPublisher;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.CurrentWeather;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import br.com.artheus.kairos.weather.WeatherResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClimateService Unit Tests")
class ClimateServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RiskCalculationPublisher riskCalculationPublisher;

    @Mock
    private OpenMeteoClient openMeteoClient;

    @Mock
    private ClimateDataRepository climateDataRepository;

    @InjectMocks
    private ClimateService climateService;

    @Nested
    @DisplayName("Tests for findLatestByCity")
    class FindLatestByCityTests {

        @Test
        @DisplayName("Should return ClimateDataSummary when latest climate data exists for the city")
        void shouldReturnSummaryWhenDataExists() {
            String cityId = "city-123";
            LocalDateTime now = LocalDateTime.now();

            ClimateData climateData = ClimateData.builder()
                    .temperature(25.0)
                    .humidity(60.0)
                    .rainVolume(0.0)
                    .windSpeed(10.0)
                    .riskLevel(RiskLevel.LOW)
                    .collectedAt(now)
                    .build();

            when(climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId))
                    .thenReturn(Optional.of(climateData));

            ClimateDataSummary result = climateService.findLatestByCity(cityId);

            assertThat(result).isNotNull();
            assertThat(result.temperature()).isEqualTo(25.0);
            assertThat(result.humidity()).isEqualTo(60.0);
            assertThat(result.rainVolume()).isEqualTo(0.0);
            assertThat(result.windSpeed()).isEqualTo(10.0);
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(result.collectedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no climate data is found")
        void shouldThrowExceptionWhenNoDataFound() {
            String cityId = "city-empty";
            when(climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> climateService.findLatestByCity(cityId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("This city has no climate data");
        }
    }

    @Nested
    @DisplayName("Tests for processClimateFetch")
    class ProcessClimateFetchTests {

        @Test
        @DisplayName("Should successfully fetch from client, save locally, and publish to risk calculator")
        void shouldProcessClimateFetchSuccessfully() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);

            CurrentWeather currentWeather = new CurrentWeather(
                    "2026-07-17T12:00",
                    22.5,
                    65,
                    2.0,
                    15.0
            );

            WeatherResponse weatherResponse = new WeatherResponse(
                    -23.55,
                    -46.63,
                    currentWeather
            );

            City mockCity = mock(City.class);

            when(openMeteoClient.getWeather(request.latitude(), request.longitude()))
                    .thenReturn(weatherResponse);
            when(cityRepository.getReferenceById(request.cityId()))
                    .thenReturn(mockCity);

            climateService.processClimateFetch(request);

            ArgumentCaptor<ClimateData> climateDataCaptor = ArgumentCaptor.forClass(ClimateData.class);
            verify(climateDataRepository).save(climateDataCaptor.capture());

            ClimateData savedData = climateDataCaptor.getValue();
            assertThat(savedData.getTemperature()).isEqualTo(22.5);
            assertThat(savedData.getHumidity()).isEqualTo(65);
            assertThat(savedData.getCity()).isEqualTo(mockCity);

            ArgumentCaptor<RiskMessage> riskMessageCaptor = ArgumentCaptor.forClass(RiskMessage.class);
            verify(riskCalculationPublisher).publish(riskMessageCaptor.capture());

            RiskMessage publishedMessage = riskMessageCaptor.getValue();
            assertThat(publishedMessage.cityId()).isEqualTo("city-123");
            assertThat(publishedMessage.temperature()).isEqualTo(22.5);
            assertThat(publishedMessage.humidity()).isEqualTo(65);
        }

        @Test
        @DisplayName("Should throw BusinessException when request is null or missing cityId")
        void shouldThrowBusinessExceptionWhenRequestIsInvalid() {
            assertThatThrownBy(() -> climateService.processClimateFetch(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid climate fetch request or missing city ID");

            ClimateFetchRequest invalidRequest = new ClimateFetchRequest(null, 0.0, 0.0);
            assertThatThrownBy(() -> climateService.processClimateFetch(invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid climate fetch request or missing city ID");
        }

        @Test
        @DisplayName("Should throw ExternalServiceException when integration client throws WebClient exceptions")
        void shouldThrowExternalServiceExceptionWhenClientFails() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);
            WebClientResponseException mockException = mock(WebClientResponseException.class);

            when(openMeteoClient.getWeather(anyDouble(), anyDouble()))
                    .thenThrow(mockException);

            assertThatThrownBy(() -> climateService.processClimateFetch(request))
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessageContaining("Unable to fetch weather data for city ID: city-123");

            verify(climateDataRepository, never()).save(any());
            verify(riskCalculationPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Should not publish risk message and should propagate exception when saving climate data fails")
        void shouldNotPublishWhenSavingClimateDataFails() {
            ClimateFetchRequest request = new ClimateFetchRequest("city-123", -23.55, -46.63);

            CurrentWeather currentWeather = new CurrentWeather(
                    "2026-07-17T12:00",
                    22.5,
                    65,
                    2.0,
                    15.0
            );

            WeatherResponse weatherResponse = new WeatherResponse(
                    -23.55,
                    -46.63,
                    currentWeather
            );

            City mockCity = mock(City.class);

            when(openMeteoClient.getWeather(request.latitude(), request.longitude()))
                    .thenReturn(weatherResponse);
            when(cityRepository.getReferenceById(request.cityId()))
                    .thenReturn(mockCity);

            org.springframework.dao.DataIntegrityViolationException dbException =
                    new org.springframework.dao.DataIntegrityViolationException("Database constraint violation");

            when(climateDataRepository.save(any(ClimateData.class)))
                    .thenThrow(dbException);

            assertThatThrownBy(() -> climateService.processClimateFetch(request))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                    .hasMessageContaining("Database constraint violation");

            verify(riskCalculationPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("Tests for saveCalculatedRisk")
    class SaveCalculatedRiskTests {

        @Test
        @DisplayName("Should update risk level and save when matching climate data is found")
        void shouldUpdateRiskLevelSuccessfully() {
            String cityId = "city-123";
            ClimateData climateData = spy(ClimateData.builder().build());

            when(climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId))
                    .thenReturn(Optional.of(climateData));

            climateService.saveCalculatedRisk(cityId, RiskLevel.HIGH);

            verify(climateData).updateRiskLevel(RiskLevel.HIGH);
            verify(climateDataRepository).save(climateData);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when trying to update risk for non-existent climate data")
        void shouldThrowExceptionWhenNoClimateDataToUpdate() {
            String cityId = "city-ghost";
            when(climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> climateService.saveCalculatedRisk(cityId, RiskLevel.HIGH))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("This city has no climate data");

            verify(climateDataRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for saveClimateData")
    class SaveClimateDataTests {

        @Test
        @DisplayName("Should build and persist ClimateData correctly from a RiskMessage")
        void shouldSaveClimateDataFromMessage() {
            RiskMessage message = new RiskMessage("city-123", 30.0, 50, 0.0, 5.0);
            City mockCity = mock(City.class);

            when(cityRepository.getReferenceById("city-123")).thenReturn(mockCity);

            climateService.saveClimateData(message);

            ArgumentCaptor<ClimateData> captor = ArgumentCaptor.forClass(ClimateData.class);
            verify(climateDataRepository).save(captor.capture());

            ClimateData saved = captor.getValue();
            assertThat(saved.getCity()).isEqualTo(mockCity);
            assertThat(saved.getTemperature()).isEqualTo(30.0);

            assertThat(saved.getHumidity()).isEqualTo(50.0);
            assertThat(saved.getRainVolume()).isEqualTo(0.0);
            assertThat(saved.getWindSpeed()).isEqualTo(5.0);
            assertThat(saved.getCollectedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}