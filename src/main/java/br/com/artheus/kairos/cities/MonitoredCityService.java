package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.plan.Plan;
import br.com.artheus.kairos.plan.PlanRepository;
import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.GeocodingResponse;
import br.com.artheus.kairos.weather.GeocodingResult;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoredCityService implements CityProvider {

    private final PlanRepository planRepository;
    private final UserCityRepository userCityRepository;
    private final CityRepository cityRepository;
    private final OpenMeteoClient openMeteoClient;

    /**
     * Fetches a city from the external geocoding API (Open-Meteo).
     * The  OpenMeteoClient already applies automatic retries @Retryable
     * for transient network failures or 5xx errors. If all attempts are exhausted,
     * the raw WebClient exception is translated here into an ExternalServiceException,
     * ensuring that the controller/exception handler layer does not need to know
     * HTTP client implementation details.
     */
    public GeocodingResponse searchCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new BusinessException("City name cannot be null or empty");
        }
        try {
            return openMeteoClient.searchCity(cityName);
        } catch (WebClientRequestException | WebClientResponseException ex) {
            throw new ExternalServiceException(
                    "GEOCODING_SERVICE_UNAVAILABLE",
                    "Unable to fetch the city right now. Please try again in a moment.",
                    ex
            );
        }
    }

    @Transactional
    public MonitoredCityResponse addCityToMonitor(String cityName, String userId) {
        validateUserPlanLimit(userId);

        GeocodingResult geocodingResult = fetchCityFromExternalApi(cityName);
        City city = getOrCreateCity(geocodingResult);

        validateUniqueMonitoring(userId, city.getId());

        UserCity userCity = saveUserCityAssociation(userId, city.getId());

        return MonitoredCityResponse.from(city, userCity);
    }

    @Transactional(readOnly = true)
    public List<MonitoredCityResponse> listMyCities(String userId) {
        List<UserCity> userCities = userCityRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        if (userCities.isEmpty()) {
            return List.of();
        }

        List<String> cityIds = userCities.stream()
                .map(UserCity::getCityId)
                .toList();

        List<City> cities = cityRepository.findAllById(cityIds);

        Map<String, City> cityMap = cities.stream()
                .collect(Collectors.toMap(City::getId, city -> city));

        return userCities.stream()
                .map(userCity -> {
                    City city = cityMap.get(userCity.getCityId());

                    // Validation and Inconsistency Log
                    if (city == null) {
                        log.error("[DATA INCONSISTENCY] UserCity with ID {} points to a non-existent cityId {} in the database!",
                                userCity.getId(), userCity.getCityId());
                        return null; // Temporarily returns null to be filtered below
                    }

                    return MonitoredCityResponse.from(city, userCity);
                })
                .filter(Objects::nonNull) // Removes mappings that failed
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityLocation> findActiveCities() {
        List<UserCity> userCities = userCityRepository.findAllByActiveTrue();

        if (userCities.isEmpty()) {
            return List.of();
        }

        List<String> cityIds = userCities.stream()
                .map(UserCity::getCityId)
                .toList();

        List<City> cities = cityRepository.findAllById(cityIds);

        Map<String, City> cityMap = cities.stream()
                .collect(Collectors.toMap(City::getId, city -> city));

        return userCities.stream()
                .map(userCity -> {
                    City city = cityMap.get(userCity.getCityId());

                    if (city == null) {
                        log.error("[DATA INCONSISTENCY] Active UserCity with ID {} points to a non-existent cityId {} in the database!",
                                userCity.getId(), userCity.getCityId());
                        return null;
                    }

                    return new CityLocation(city.getId(), city.getName(), city.getLatitude(), city.getLongitude());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public CityLocation findCityById(String cityId) {
        City city = fetchCityOrThrow(cityId);
        return new CityLocation(city.getId(), city.getName(), city.getLatitude(), city.getLongitude());
    }

    // ==========================================
    // Aux Methods
    // ==========================================

    private void validateUserPlanLimit(String userId) {
        Plan plan = planRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        long cityCount = userCityRepository.countByUserIdAndActiveTrue(userId);

        if (plan.hasReachedCityLimit(cityCount)) {
            throw new BusinessException("City limit reached for your plan");
        }
    }

    private GeocodingResult fetchCityFromExternalApi(String cityName) {
        GeocodingResponse geocoding = this.searchCity(cityName);

        if (geocoding.results() == null || geocoding.results().isEmpty()) {
            throw new BusinessException("City not found");
        }

        return geocoding.results().get(0);
    }

    private City getOrCreateCity(GeocodingResult result) {
        return cityRepository.findByName(result.name())
                .orElseGet(() -> {
                    City newCity = City.builder()
                            .name(result.name())
                            .state(result.admin1())
                            .latitude(result.latitude())
                            .longitude(result.longitude())
                            .country(result.country())
                            .build();
                    return cityRepository.save(newCity);
                });
    }

    private void validateUniqueMonitoring(String userId, String cityId) {
        boolean alreadyMonitoring = userCityRepository
                .findByUserIdAndCityIdAndActiveTrue(userId, cityId)
                .isPresent();

        if (alreadyMonitoring) {
            throw new BusinessException("You are already monitoring this city");
        }
    }

    private UserCity saveUserCityAssociation(String userId, String cityId) {
        UserCity userCity = UserCity.builder()
                .userId(userId)
                .cityId(cityId)
                .build();
        return userCityRepository.save(userCity);
    }

    private City fetchCityOrThrow(String cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + cityId));
    }
}