package br.com.artheus.kairos.shared.contract.climate;

public record ClimateFetchRequest(
        String cityId,
        Double latitude,
        Double longitude
) {
}
