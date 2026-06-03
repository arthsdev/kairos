package br.com.artheus.kairos.climate;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitoredCityRepository extends JpaRepository<MonitoredCity, String> {

    List<MonitoredCity> findAllByState(String state);

    List<MonitoredCity> findAllByActive(boolean active);

    List<MonitoredCity> findAllByRequestedBy(String requestedBy);

    long countByRequestedByAndActiveTrue(String userId);

    boolean existsByNameAndRequestedByAndActiveTrue(String name, String userId);
}
