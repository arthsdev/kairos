package br.com.artheus.kairos.climate;

public record GeocodingResult(
        String name,
        double latitude,
        double longitude,
        String admin1,
        String country
) {}