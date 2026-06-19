package br.com.artheus.kairos.shared.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // connectTimeout: Maximum time allowed to establish a TCP connection with the external server.
        // responseTimeout: Maximum time allowed to receive a response after the connection is established.
        // Conservative values — external calls (Open-Meteo) have @Retryable configured,
        // so failing fast here is intentional so that the retry triggers without blocking the thread.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}