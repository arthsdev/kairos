package br.com.artheus.kairos.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Original queues
    public static final String FETCH_CLIMATE_DATA = "fetch.climate.data";
    public static final String CALCULATE_RISK = "calculate.risk";

    // Original exchanges
    public static final String CLIMATE_EXCHANGE = "exchange.climate";
    public static final String RISK_EXCHANGE = "exchange.risk";

    // DLQ and DLX
    public static final String FETCH_CLIMATE_DATA_DLQ = "fetch.climate.data.dlq";
    public static final String CALCULATE_RISK_DLQ = "calculate.risk.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "exchange.dlx";

    // --- ORIGINAL QUEUES (With DLX and Routing Key configured) ---
    @Bean
    public Queue fetchClimateDataQueue() {
        return QueueBuilder.durable(FETCH_CLIMATE_DATA)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FETCH_CLIMATE_DATA_DLQ) // Routes directly using the DLQ name
                .build();
    }

    @Bean
    public Queue calculateRiskQueue() {
        return QueueBuilder.durable(CALCULATE_RISK)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CALCULATE_RISK_DLQ) // Routes directly using the DLQ name
                .build();
    }

    // --- DEAD LETTER QUEUES ---
    @Bean
    public Queue fetchClimateDataDlq() {
        return QueueBuilder.durable(FETCH_CLIMATE_DATA_DLQ).build();
    }

    @Bean
    public Queue calculateRiskDlq() {
        return QueueBuilder.durable(CALCULATE_RISK_DLQ).build();
    }

    // --- EXCHANGES ---
    @Bean
    public DirectExchange climateExchange() {
        return new DirectExchange(CLIMATE_EXCHANGE);
    }

    @Bean
    public DirectExchange riskExchange() {
        return new DirectExchange(RISK_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // --- ORIGINAL BINDINGS ---
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

    // --- DLQ BINDINGS ---
    @Bean
    public Binding bindingFetchDlq(Queue fetchClimateDataDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(fetchClimateDataDlq)
                .to(deadLetterExchange)
                .with(FETCH_CLIMATE_DATA_DLQ); // Binds the DLQ using its own name as routing key
    }

    @Bean
    public Binding bindingRiskDlq(Queue calculateRiskDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(calculateRiskDlq)
                .to(deadLetterExchange)
                .with(CALCULATE_RISK_DLQ); // Binds the DLQ using its own name as routing key
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}