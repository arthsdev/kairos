package br.com.artheus.kairos.climate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        double latitude,
        double longitude,
        @JsonProperty("current") CurrentWeather currentWeather
) {}