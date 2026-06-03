package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.plan.Plan;
import br.com.artheus.kairos.plan.PlanRepository;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClimateService {

    private final OpenMeteoClient openMeteoClient;

    private final MonitoredCityRepository monitoredCityRepository;

    private final PlanRepository planRepository;

    private final ClimateDataRepository climateDataRepository;

    public GeocodingResponse searchCity(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            throw new BusinessException("City name cannot be null or empty");
        }
        return openMeteoClient.searchCity(cityName);
    }


    public MonitoredCityResponse addCityToMonitor(String cityName, String userId) {

        Plan plan = planRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        long cityCount = monitoredCityRepository.countByRequestedByAndActiveTrue(userId);

        if (plan.hasReachedCityLimit(cityCount)) {
            throw new BusinessException("City limit reached for your plan");
        }

        GeocodingResponse geocoding = openMeteoClient.searchCity(cityName);

        if (geocoding.results() == null || geocoding.results().isEmpty()) {
            throw new BusinessException("City not found");
        }

        GeocodingResult result = geocoding.results().get(0);

        if (monitoredCityRepository.existsByNameAndRequestedByAndActiveTrue(cityName, userId)) {
            throw new BusinessException("City already being monitored");
        }

        MonitoredCity city = MonitoredCity.builder()
                .name(result.name())
                .state(result.admin1())
                .latitude(result.latitude())
                .longitude(result.longitude())
                .requestedBy(userId)
                .build();

        monitoredCityRepository.save(city);

        return MonitoredCityResponse.from(city);
    }

    public List<MonitoredCityResponse> listMyCities(String userId) {
        return monitoredCityRepository.findAllByRequestedBy(userId)
                .stream()
                .map(MonitoredCityResponse::from)
                .toList();

    }

    public void fetchClimateDataForAllCities() {
        List<MonitoredCity> activeCities = monitoredCityRepository.findAllByActive(true);
        List<ClimateData> climateDataList = new ArrayList<>();

        for (MonitoredCity city : activeCities) {
            try {
                WeatherResponse weather = openMeteoClient.getWeather(city.getLatitude(), city.getLongitude());

                climateDataList.add(ClimateData.builder()
                        .city(city)
                        .collectedAt(LocalDateTime.now())
                        .temperature(weather.currentWeather().temperature())
                        .humidity(weather.currentWeather().humidity())
                        .rainVolume(weather.currentWeather().rainVolume())
                        .windSpeed(weather.currentWeather().windSpeed())
                        .build());
            } catch (Exception e) {
                log.error("Error retrieving weather data for city: {}", city.getName(), e);
            }
        }

        climateDataRepository.saveAll(climateDataList);
    }
}
