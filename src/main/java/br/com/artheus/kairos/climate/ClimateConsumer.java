package br.com.artheus.kairos.climate;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.climate.ClimateFetchRequest;
import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClimateConsumer {

    private final ClimateService climateService;

    @RabbitListener(queues = RabbitMQConfig.FETCH_CLIMATE_DATA)
    public void fetchClimateData(ClimateFetchRequest climateFetchRequest) {
        try {
            log.info("Processing climate fetch for city: {}", climateFetchRequest.cityId());
            climateService.processClimateFetch(climateFetchRequest);
        } catch (BusinessException | ResourceNotFoundException | ExternalServiceException e) {
            log.error("Permanent failure or retries exhausted for city {}. Routing to DLQ. Reason: {}",
                    climateFetchRequest.cityId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Permanent failure or retry exhausted in climate processing.", e);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation for city {}. Routing to DLQ. Reason: {}",
                    climateFetchRequest.cityId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Data integrity violation.", e);

        } catch (Exception e) {
            log.error("Unexpected error processing city {}. Behavior depends on container requeue configuration. Reason: {}",
                    climateFetchRequest.cityId(), e.getMessage(), e);
            // Re-throwing lets the container handle it (will requeue if default-requeue-rejected is true)
            throw e;
        }
    }
}