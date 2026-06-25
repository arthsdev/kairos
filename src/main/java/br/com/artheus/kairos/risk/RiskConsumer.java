package br.com.artheus.kairos.risk;

import br.com.artheus.kairos.shared.config.RabbitMQConfig;
import br.com.artheus.kairos.shared.contract.risk.RiskMessage;
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
public class RiskConsumer {

    private final RiskService riskService;

    @RabbitListener(queues = RabbitMQConfig.CALCULATE_RISK)
    public void fetchCalculatedRisk(RiskMessage riskMessage) {
        try {
            log.info("Processing risk calculation for city: {}", riskMessage.cityId());
            riskService.processRiskCalculation(riskMessage);
        } catch (BusinessException | ResourceNotFoundException | ExternalServiceException e) {
            log.error("Permanent failure or retries exhausted for city {}. Routing to DLQ. Reason: {}",
                    riskMessage.cityId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Permanent failure or retry exhausted in risk processing.", e);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation for city {}. Routing to DLQ. Reason: {}",
                    riskMessage.cityId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Data integrity violation in risk processing.", e);

        } catch (Exception e) {
            log.error("Unexpected error processing city {}. Behavior depends on container requeue configuration. Reason: {}",
                    riskMessage.cityId(), e.getMessage(), e);
            throw e;
        }
    }
}