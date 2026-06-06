package br.com.artheus.kairos.cities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, String> {

    List<City> findAllByState(String state);

    Optional<City> findByName(String name);

    List<City> findAllByCountry(String name);

}
