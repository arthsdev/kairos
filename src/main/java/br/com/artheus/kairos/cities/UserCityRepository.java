package br.com.artheus.kairos.cities;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserCityRepository extends JpaRepository<UserCity, String> {

    List<UserCity> findAllByOrderByCreatedAtDesc();

    List<UserCity> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<UserCity> findAllByCityIdOrderByCreatedAtDesc(String cityId);

    long countByUserIdAndActiveTrue(String userId);

    Optional<UserCity> findByUserIdAndCityIdAndActiveTrue(String userId, String cityId);

    List<UserCity> findAllByActiveTrue();


}