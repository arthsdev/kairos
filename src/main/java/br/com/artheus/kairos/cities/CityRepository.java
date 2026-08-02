package br.com.artheus.kairos.cities;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, String> {

    List<City> findAllByState(String state);

    Optional<City> findByName(String name);

    List<City> findAllByCountry(String name);

    @Query("SELECT c FROM City c WHERE c.name = :name " +
            "AND c.latitude BETWEEN :latitude - 0.0001 AND :latitude + 0.0001 " +
            "AND c.longitude BETWEEN :longitude - 0.0001 AND :longitude + 0.0001")
    Optional<City> findByNameAndApproximateLocation(
            @Param("name") String name,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude
    );

}
