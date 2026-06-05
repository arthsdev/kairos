package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.contract.cities.CityLocation;
import br.com.artheus.kairos.shared.contract.cities.CityProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.cities.MonitoredCity;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import br.com.artheus.kairos.weather.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClimateService implements ClimateDataProvider {

    private final CityProvider cityProvider;

    private final OpenMeteoClient openMeteoClient;

    private final ClimateDataRepository climateDataRepository;

    public void fetchClimateDataForAllCities() {
        List<CityLocation> activeCities = cityProvider.findActiveCities();
        List<ClimateData> climateDataList = new ArrayList<>();

        for (CityLocation city : activeCities) {
            try {
                WeatherResponse weather = openMeteoClient.getWeather(city.latitude(), city.longitude());

                MonitoredCity cityRef = MonitoredCity.builder()
                        .id(city.id())
                        .build();

                climateDataList.add(ClimateData.builder()
                        .city(cityRef)
                        .collectedAt(LocalDateTime.now())
                        .temperature(weather.currentWeather().temperature())
                        .humidity(weather.currentWeather().humidity())
                        .rainVolume(weather.currentWeather().rainVolume())
                        .windSpeed(weather.currentWeather().windSpeed())
                        .build());
            } catch (Exception e) {
                log.error("Error retrieving weather data for city: {}", city.name(), e);
            }
        }

        climateDataRepository.saveAll(climateDataList);
    }

    public ClimateDataSummary findLatestByCity(String cityId){

        return climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId)
                .map(climateData -> new ClimateDataSummary(
                        climateData.getTemperature(),
                        climateData.getHumidity(),
                        climateData.getRainVolume(),
                        climateData.getWindSpeed(),
                        climateData.getRiskLevel(),
                        climateData.getCollectedAt()
                ))
                .orElseThrow(() -> new ResourceNotFoundException("This city has no climate data"));
    }
}
