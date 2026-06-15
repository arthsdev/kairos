package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.risk.RiskProducer;
import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataWriter;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
import br.com.artheus.kairos.weather.OpenMeteoClient;
import br.com.artheus.kairos.weather.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClimateConsumer {

    private final OpenMeteoClient openMeteoClient;
    private final RiskProducer riskProducer;
    private final ClimateDataWriter climateDataWriter;

    @RabbitListener(queues = RabbitMQConfig.FETCH_CLIMATE_DATA)
    public void fetchClimateData(ClimateFetchRequest climateFetchRequest) {

        WeatherResponse response = openMeteoClient.getWeather(climateFetchRequest.latitude(), climateFetchRequest.longitude());

        RiskMessage riskMessage = new RiskMessage(
                climateFetchRequest.cityId(),
                response.currentWeather().temperature(),
                response.currentWeather().humidity(),
                response.currentWeather().rainVolume(),
                response.currentWeather().windSpeed()
        );

        climateDataWriter.saveClimateData(riskMessage);

        riskProducer.publishCalculatedRisk(riskMessage);

        log.info("Climate data processed for city: {}", climateFetchRequest.cityId());
    }
}
