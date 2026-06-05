package br.com.artheus.kairos.shared.contract.cities;

public record CityLocation(
        String id,
        String name,
        Double latitude,
        Double longitude
) {
}
