package br.com.artheus.kairos.cities;

public record MonitoredCityResponse(

         String id,

         String name,

         String state,

         Double latitude,

         Double longitude,

         String country,

         boolean active

) {

    public static MonitoredCityResponse from(City city, UserCity userCity) {
        return new MonitoredCityResponse(
                city.getId(),
                city.getName(),
                city.getState(),
                city.getLatitude(),
                city.getLongitude(),
                city.getCountry(),
                userCity.isActive()
        );
    }
}
