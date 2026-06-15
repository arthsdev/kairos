package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClimateProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishClimateData(ClimateFetchRequest climateFetchRequest) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CLIMATE_EXCHANGE,
                RabbitMQConfig.FETCH_CLIMATE_DATA, // routing key
                climateFetchRequest
        );
    }

}
