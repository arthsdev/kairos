package br.com.artheus.kairos.climate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrentWeather(
        String time,
        @JsonProperty("temperature_2m") double temperature,
        @JsonProperty("relative_humidity_2m") int humidity,
        @JsonProperty("precipitation") double rainVolume,
        @JsonProperty("wind_speed_10m") double windSpeed
) {}