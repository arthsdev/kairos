package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OccurrenceRepository extends JpaRepository<Occurrence, String> {

    List<Occurrence> findByUserId(String userId);

    List<Occurrence> findAllByOrderByCreatedAtDesc();

    List<Occurrence> findByUserIdAndStatus(String userId, OccurrenceStatus status);

    List<Occurrence> findAllByStatus(OccurrenceStatus status);

    List<Occurrence> findByCityIdAndStatusIn(String cityId, List<OccurrenceStatus> statuses);
}
