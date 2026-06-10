package br.com.artheus.kairos.climate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClimateDataRepository extends JpaRepository<ClimateData, String> {

    List<ClimateData> findByCityId(String cityId);

    Optional<ClimateData> findFirstByCityIdOrderByCollectedAtDesc(String cityId);
}
