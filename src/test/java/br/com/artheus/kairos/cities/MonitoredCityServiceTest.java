package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.plan.Plan;
import br.com.artheus.kairos.plan.PlanRepository;
import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.GeocodingResponse;
import br.com.artheus.kairos.weather.GeocodingResult;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoredCityService Unit Tests")
class MonitoredCityServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserCityRepository userCityRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private OpenMeteoClient openMeteoClient;

    @InjectMocks
    private MonitoredCityService monitoredCityService;

    @Nested
    @DisplayName("Tests for searchCity")
    class SearchCityTests {

        @Test
        @DisplayName("Should return GeocodingResponse when valid city name is provided")
        void shouldReturnResponseWhenNameIsValid() {
            String cityName = "London";
            GeocodingResponse expectedResponse = new GeocodingResponse(List.of());
            when(openMeteoClient.searchCity(cityName)).thenReturn(expectedResponse);

            GeocodingResponse response = monitoredCityService.searchCity(cityName);

            assertThat(response).isNotNull();
            verify(openMeteoClient, times(1)).searchCity(cityName);
        }

        @Test
        @DisplayName("Should throw BusinessException when city name is blank or null")
        void shouldThrowExceptionWhenNameIsInvalid() {
            assertThatThrownBy(() -> monitoredCityService.searchCity(""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("City name cannot be null or empty");

            assertThatThrownBy(() -> monitoredCityService.searchCity(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("City name cannot be null or empty");

            verifyNoInteractions(openMeteoClient);
        }

        @Test
        @DisplayName("Should map WebClient exceptions to ExternalServiceException")
        void shouldMapWebClientExceptionsToExternalServiceException() {
            String cityName = "Paris";
            WebClientResponseException mockEx = WebClientResponseException.create(
                    503, "Service Unavailable", HttpHeaders.EMPTY, null, null
            );
            when(openMeteoClient.searchCity(cityName)).thenThrow(mockEx);

            assertThatThrownBy(() -> monitoredCityService.searchCity(cityName))
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessageContaining("Unable to fetch the city right now")
                    .hasRootCause(mockEx);
        }
    }

    @Nested
    @DisplayName("Tests for addCityToMonitor")
    class AddCityToMonitorTests {

        private final String userId = "user-123";
        private final String cityName = "Joinville";

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user plan is missing")
        void shouldThrowExceptionWhenPlanNotFound() {
            when(planRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> monitoredCityService.addCityToMonitor(cityName, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Plan not found");

            verifyNoInteractions(openMeteoClient, cityRepository);
        }

        @Test
        @DisplayName("Should throw BusinessException when user has reached plan city limit")
        void shouldThrowExceptionWhenLimitReached() {
            Plan plan = mock(Plan.class);
            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(userCityRepository.countByUserIdAndActiveTrue(userId)).thenReturn(5L);
            when(plan.hasReachedCityLimit(5L)).thenReturn(true);

            assertThatThrownBy(() -> monitoredCityService.addCityToMonitor(cityName, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("City limit reached for your plan");

            verifyNoInteractions(openMeteoClient, cityRepository);
        }

        @Test
        @DisplayName("Should throw BusinessException when API returns no geocoding results")
        void shouldThrowExceptionWhenCityNotFoundExternally() {
            Plan plan = mock(Plan.class);
            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(userCityRepository.countByUserIdAndActiveTrue(userId)).thenReturn(2L);
            when(plan.hasReachedCityLimit(2L)).thenReturn(false);

            GeocodingResponse emptyResponse = new GeocodingResponse(Collections.emptyList());
            when(openMeteoClient.searchCity(cityName)).thenReturn(emptyResponse);

            assertThatThrownBy(() -> monitoredCityService.addCityToMonitor(cityName, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("City not found");

            verifyNoInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should throw BusinessException when user is already monitoring the target city")
        void shouldThrowExceptionWhenAlreadyMonitoring() {
            Plan plan = mock(Plan.class);
            City existingCity = City.builder().id("city-99").name(cityName).build();
            GeocodingResult result = new GeocodingResult(cityName, -26.3, -48.8, "SC", "Brazil");

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(userCityRepository.countByUserIdAndActiveTrue(userId)).thenReturn(1L);
            when(plan.hasReachedCityLimit(1L)).thenReturn(false);
            when(openMeteoClient.searchCity(cityName)).thenReturn(new GeocodingResponse(List.of(result)));
            when(cityRepository.findByName(cityName)).thenReturn(Optional.of(existingCity));

            when(userCityRepository.findByUserIdAndCityIdAndActiveTrue(userId, "city-99"))
                    .thenReturn(Optional.of(new UserCity()));

            assertThatThrownBy(() -> monitoredCityService.addCityToMonitor(cityName, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("You are already monitoring this city");

            verify(userCityRepository, never()).save(any(UserCity.class));
        }

        @Test
        @DisplayName("Should reuse existing city entry and only save UserCity association")
        void shouldReuseExistingCityWhenFoundInDatabase() {
            Plan plan = mock(Plan.class);
            City dbCity = City.builder().id("city-exists").name(cityName).build();
            GeocodingResult result = new GeocodingResult(cityName, -26.3, -48.8, "SC", "Brazil");
            UserCity savedUserCity = UserCity.builder()
                    .id("uc-123")
                    .userId(userId)
                    .city(City.builder().id("city-exists").build())
                    .build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(userCityRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L);
            when(plan.hasReachedCityLimit(0L)).thenReturn(false);
            when(openMeteoClient.searchCity(cityName)).thenReturn(new GeocodingResponse(List.of(result)));
            when(cityRepository.findByName(cityName)).thenReturn(Optional.of(dbCity));
            when(userCityRepository.findByUserIdAndCityIdAndActiveTrue(userId, "city-exists")).thenReturn(Optional.empty());
            when(userCityRepository.save(any(UserCity.class))).thenReturn(savedUserCity);

            MonitoredCityResponse response = monitoredCityService.addCityToMonitor(cityName, userId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo("city-exists");
            verify(cityRepository, never()).save(any(City.class));
            verify(userCityRepository, times(1)).save(any(UserCity.class));
        }

        @Test
        @DisplayName("Should persist a new City entity when it does not exist locally")
        void shouldCreateNewCityAndAssociateWithUser() {
            Plan plan = mock(Plan.class);
            GeocodingResult result = new GeocodingResult(cityName, -26.3, -48.8, "SC", "Brazil");
            City newCity = City.builder().id("city-new").name(cityName).build();
            UserCity savedUserCity = UserCity.builder()
                    .id("uc-789")
                    .userId(userId)
                    .city(City.builder().id("city-new").build())
                    .build();

            when(planRepository.findByUserId(userId)).thenReturn(Optional.of(plan));
            when(userCityRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L);
            when(plan.hasReachedCityLimit(0L)).thenReturn(false);
            when(openMeteoClient.searchCity(cityName)).thenReturn(new GeocodingResponse(List.of(result)));
            when(cityRepository.findByName(cityName)).thenReturn(Optional.empty());
            when(cityRepository.save(any(City.class))).thenReturn(newCity);
            when(userCityRepository.findByUserIdAndCityIdAndActiveTrue(userId, "city-new")).thenReturn(Optional.empty());
            when(userCityRepository.save(any(UserCity.class))).thenReturn(savedUserCity);

            MonitoredCityResponse response = monitoredCityService.addCityToMonitor(cityName, userId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo("city-new");
            verify(cityRepository, times(1)).save(any(City.class));
            verify(userCityRepository, times(1)).save(any(UserCity.class));
        }
    }

    @Nested
    @DisplayName("Tests for listMyCities")
    class ListMyCitiesTests {

        @Test
        @DisplayName("Should return mapped city list and filter out inconsistent data gracefully")
        void shouldReturnMappedCitiesAndFilterInconsistencies() {
            String userId = "user-1";
            UserCity validAssociation = UserCity.builder()
                    .id("uc-1")
                    .userId(userId)
                    .city(City.builder().id("city-valid").build())
                    .build();
            UserCity brokenAssociation = UserCity.builder()
                    .id("uc-2")
                    .userId(userId)
                    .city(City.builder().id("city-ghost").build())
                    .build();

            City validCity = City.builder().id("city-valid").name("Blumenau").build();

            when(userCityRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(validAssociation, brokenAssociation));
            when(cityRepository.findAllById(anyList())).thenReturn(List.of(validCity));

            List<MonitoredCityResponse> result = monitoredCityService.listMyCities(userId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("city-valid");
        }

        @Test
        @DisplayName("Should return empty list when user is not monitoring any city")
        void shouldReturnEmptyListWhenNoAssociationsFound() {
            String userId = "user-empty";
            when(userCityRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());

            List<MonitoredCityResponse> result = monitoredCityService.listMyCities(userId);

            assertThat(result).isEmpty();
            verifyNoInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should return empty list when all associations are data inconsistencies (all filtered out)")
        void shouldReturnEmptyListWhenAllCitiesAreInconsistent() {
            String userId = "user-total-ghost";
            UserCity brokenUc1 = UserCity.builder()
                    .id("uc-10")
                    .userId(userId)
                    .city(City.builder().id("ghost-1").build())
                    .build();
            UserCity brokenUc2 = UserCity.builder()
                    .id("uc-20")
                    .userId(userId)
                    .city(City.builder().id("ghost-2").build())
                    .build();

            when(userCityRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(brokenUc1, brokenUc2));
            when(cityRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

            List<MonitoredCityResponse> result = monitoredCityService.listMyCities(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests for findActiveCities")
    class FindActiveCitiesTests {

        @Test
        @DisplayName("Should return list of active CityLocations and skip inconsistent items")
        void shouldReturnActiveCitiesAndFilterGhostEntries() {
            UserCity activeUc = UserCity.builder()
                    .id("uc-1")
                    .city(City.builder().id("city-active").build())
                    .build();
            UserCity ghostUc = UserCity.builder()
                    .id("uc-2")
                    .city(City.builder().id("city-ghost").build())
                    .build();
            City city = City.builder().id("city-active").name("Timbó").latitude(-26.8).longitude(-49.2).build();

            when(userCityRepository.findAllByActiveTrue()).thenReturn(List.of(activeUc, ghostUc));
            when(cityRepository.findAllById(anyList())).thenReturn(List.of(city));

            List<CityLocation> result = monitoredCityService.findActiveCities();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("city-active");
        }

        @Test
        @DisplayName("Should return empty list when there are no active monitoring links")
        void shouldReturnEmptyListWhenNoActiveCitiesFound() {
            when(userCityRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

            List<CityLocation> result = monitoredCityService.findActiveCities();

            assertThat(result).isEmpty();
            verifyNoInteractions(cityRepository);
        }

        @Test
        @DisplayName("Should return empty list when all active monitorings point to non-existent cities")
        void shouldReturnEmptyListWhenAllActiveCitiesAreInconsistent() {
            UserCity brokenUc1 = UserCity.builder()
                    .id("uc-30")
                    .city(City.builder().id("ghost-3").build())
                    .build();
            UserCity brokenUc2 = UserCity.builder()
                    .id("uc-40")
                    .city(City.builder().id("ghost-4").build())
                    .build();

            when(userCityRepository.findAllByActiveTrue()).thenReturn(List.of(brokenUc1, brokenUc2));
            when(cityRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

            List<CityLocation> result = monitoredCityService.findActiveCities();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests for findCityById")
    class FindCityByIdTests {

        @Test
        @DisplayName("Should return CityLocation when entity exists in database")
        void shouldReturnCityLocationSuccessfully() {
            String cityId = "city-456";
            City city = City.builder().id(cityId).name("Gaspar").latitude(-26.9).longitude(-48.9).build();
            when(cityRepository.findById(cityId)).thenReturn(Optional.of(city));

            CityLocation location = monitoredCityService.findCityById(cityId);

            assertThat(location).isNotNull();
            assertThat(location.id()).isEqualTo(cityId);
            assertThat(location.name()).isEqualTo("Gaspar");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when city identity does not exist")
        void shouldThrowExceptionWhenCityIdNotFound() {
            String cityId = "city-missing";
            when(cityRepository.findById(cityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> monitoredCityService.findCityById(cityId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("City not found with ID");
        }
    }
}