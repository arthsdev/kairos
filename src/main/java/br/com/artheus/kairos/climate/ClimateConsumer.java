package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClimateConsumer {

    private final ClimateService climateService;

    @RabbitListener(queues = RabbitMQConfig.FETCH_CLIMATE_DATA)
    public void fetchClimateData(ClimateFetchRequest climateFetchRequest) {

        climateService.processClimateFetch(climateFetchRequest);
    }
}
