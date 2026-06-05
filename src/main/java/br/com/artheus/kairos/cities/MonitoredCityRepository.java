package br.com.artheus.kairos.cities;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoredCityRepository extends JpaRepository<MonitoredCity, String> {

    List<MonitoredCity> findAllByState(String state);

    List<MonitoredCity> findAllByActive(boolean active);

    List<MonitoredCity> findAllByRequestedBy(String requestedBy);

    long countByRequestedByAndActiveTrue(String userId);

    boolean existsByNameAndRequestedByAndActiveTrue(String name, String userId);
}
