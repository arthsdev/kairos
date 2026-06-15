package br.com.artheus.kairos.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FETCH_CLIMATE_DATA = "fetch.climate.data";
    public static final String CALCULATE_RISK = "calculate.risk";

    public static final String CLIMATE_EXCHANGE = "exchange.climate";
    public static final String RISK_EXCHANGE = "exchange.risk";

    @Bean
    public Queue fetchClimateDataQueue() {
        return new Queue(FETCH_CLIMATE_DATA, true);
    }

    @Bean
    public Queue calculateRiskQueue() {
        return new Queue(CALCULATE_RISK, true);
    }

    @Bean
    public DirectExchange climateExchange() {
        return new DirectExchange(CLIMATE_EXCHANGE);
    }

    @Bean
    public DirectExchange riskExchange() {
        return new DirectExchange(RISK_EXCHANGE);
    }

    @Bean
    public Binding bindingFetch(Queue fetchClimateDataQueue, DirectExchange climateExchange) {
        return BindingBuilder
                .bind(fetchClimateDataQueue)
                .to(climateExchange)
                .with(FETCH_CLIMATE_DATA);
    }

    @Bean
    public Binding bindingRisk(Queue calculateRiskQueue, DirectExchange riskExchange) {
        return BindingBuilder
                .bind(calculateRiskQueue)
                .to(riskExchange)
                .with(CALCULATE_RISK);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}