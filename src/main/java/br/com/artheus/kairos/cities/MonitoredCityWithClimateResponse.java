package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;

public record MonitoredCityWithClimateResponse(
        String id,
        String name,
        String state,
        Double latitude,
        Double longitude,
        String country,
        boolean active,
        ClimateDataSummary climate // null if not yet collected
) {
    public static MonitoredCityWithClimateResponse from(MonitoredCityResponse city, ClimateDataSummary climate) {
        return new MonitoredCityWithClimateResponse(
                city.id(),
                city.name(),
                city.state(),
                city.latitude(),
                city.longitude(),
                city.country(),
                city.active(),
                climate
        );
    }
}