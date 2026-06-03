package br.com.artheus.kairos.climate;

import java.time.LocalDateTime;

public record MonitoredCityResponse(

        String id,

        String name,

        String state,

        Double latitude,

        Double longitude,

        boolean active,

        LocalDateTime createdAt
) {

    public static MonitoredCityResponse from(MonitoredCity city) {
        return new MonitoredCityResponse(
                city.getId(),
                city.getName(),
                city.getState(),
                city.getLatitude(),
                city.getLongitude(),
                city.isActive(),
                city.getCreatedAt()
        );
    }
}
