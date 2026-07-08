package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OccurrenceRepository extends JpaRepository<Occurrence, String> {

    List<Occurrence> findByUserId(String userId);

    List<Occurrence> findAllByOrderByCreatedAtDesc();

    List<Occurrence> findByUserIdAndStatus(String userId, OccurrenceStatus status);

    Page<Occurrence> findAllByStatusAndDeletedAtIsNull(OccurrenceStatus status, Pageable pageable);

    List<Occurrence> findByCityIdAndStatusInAndDeletedAtIsNull(String cityId, List<OccurrenceStatus> statuses);

    Page<Occurrence> findAllByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);

    Page<Occurrence> findAllByUserIdAndStatusAndDeletedAtIsNull(String userId, OccurrenceStatus status, Pageable pageable);
}
