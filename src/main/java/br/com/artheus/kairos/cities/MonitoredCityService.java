package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.plan.Plan;
import br.com.artheus.kairos.plan.PlanRepository;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.GeocodingResponse;
import br.com.artheus.kairos.weather.GeocodingResult;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoredCityService implements CityProvider {

    private final PlanRepository planRepository;
    private final MonitoredCityRepository monitoredCityRepository;
    private final OpenMeteoClient openMeteoClient;

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

    @Override
    public List<CityLocation> findActiveCities() {
        return monitoredCityRepository.findAllByActive(true)
                .stream()
                .map(city -> new CityLocation(
                        city.getId(),
                        city.getName(),
                        city.getLatitude(),
                        city.getLongitude()
                ))
                .toList();
    }
}
