package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.cities.City;
import br.com.artheus.kairos.cities.CityRepository;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataWriter;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import br.com.artheus.kairos.shared.contract.risk.RiskCalculationPublisher;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.shared.contract.risk.RiskProvider;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import br.com.artheus.kairos.weather.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClimateService implements ClimateDataProvider, RiskProvider, ClimateDataWriter {

    private final CityRepository cityRepository;
    private final RiskCalculationPublisher riskCalculationPublisher;

    private final OpenMeteoClient openMeteoClient;

    private final ClimateDataRepository climateDataRepository;

    @Override
    public ClimateDataSummary findLatestByCity(String cityId) {

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

    public void processClimateFetch(ClimateFetchRequest request) {

        WeatherResponse response = openMeteoClient.getWeather(request.latitude(), request.longitude());

        RiskMessage riskMessage = new RiskMessage(
                request.cityId(),
                response.currentWeather().temperature(),
                response.currentWeather().humidity(),
                response.currentWeather().rainVolume(),
                response.currentWeather().windSpeed()
        );

        this.saveClimateData(riskMessage);

        riskCalculationPublisher.publish(riskMessage);

        log.info("Climate data processed for city: {}", request.cityId());

    }

    @Override
    public void saveCalculatedRisk(String cityId, RiskLevel riskLevel) {
        ClimateData climateData = climateDataRepository.findFirstByCityIdOrderByCollectedAtDesc(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("This city has no climate data"));

        climateData.updateRiskLevel(riskLevel);

        climateDataRepository.save(climateData);
    }

    @Override
    public void saveClimateData(RiskMessage message) {
        City cityReference = cityRepository.getReferenceById(message.cityId());

        ClimateData climateData = ClimateData.builder()
                .city(cityReference)
                .temperature(message.temperature())
                .humidity(message.humidity())
                .rainVolume(message.rainVolume())
                .windSpeed(message.windSpeed())
                .collectedAt(LocalDateTime.now())
                .build();

        climateDataRepository.save(climateData);
    }
}
