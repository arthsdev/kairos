package br.com.artheus.kairos.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private final WebClient webClient;

    @Retryable(
            includes = {
                    WebClientRequestException.class,
                    WebClientResponseException.InternalServerError.class,
                    WebClientResponseException.BadGateway.class,
                    WebClientResponseException.ServiceUnavailable.class,
                    WebClientResponseException.GatewayTimeout.class
            },
            maxRetries = 1,
            delay = 200,
            maxDelay = 200)
    public GeocodingResponse searchCity(String cityName) {
        return this.webClient.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?name={cityName}&count=1&language=pt", cityName)
                .retrieve()
                .bodyToMono(GeocodingResponse.class)
                .block();
    }

    @Retryable(
            includes = {
                    WebClientRequestException.class,
                    WebClientResponseException.InternalServerError.class,
                    WebClientResponseException.BadGateway.class,
                    WebClientResponseException.ServiceUnavailable.class,
                    WebClientResponseException.GatewayTimeout.class
            },
            maxRetries = 2,
            delay = 1000,
            multiplier = 2,
            maxDelay = 4000)
    public WeatherResponse getWeather(double latitude, double longitude) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.open-meteo.com")
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .block();
    }
}
