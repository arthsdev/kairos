package br.com.artheus.kairos.climate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private final WebClient webClient;

    public GeocodingResponse searchCity(String cityName) {
        return this.webClient.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?name={cityName}&count=1&language=pt", cityName)
                .retrieve()
                .bodyToMono(GeocodingResponse.class)
                .block();
    }

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
