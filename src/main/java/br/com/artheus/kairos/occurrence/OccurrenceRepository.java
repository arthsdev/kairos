package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OccurrenceRepository extends JpaRepository<Occurrence, String> {

    Page<Occurrence> findAllByStatusAndDeletedAtIsNull(OccurrenceStatus status, Pageable pageable);

    List<Occurrence> findByCityIdAndStatusInAndDeletedAtIsNull(String cityId, List<OccurrenceStatus> statuses);

    Page<Occurrence> findAllByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);

    Page<Occurrence> findAllByUserIdAndStatusAndDeletedAtIsNull(String userId, OccurrenceStatus status, Pageable pageable);

    Page<Occurrence> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT new br.com.artheus.kairos.occurrence.MapOccurrenceDTO(
            o.id, 
            o.latitude, 
            o.longitude, 
            o.category, 
            o.severity, 
            o.status
        )
        FROM Occurrence o
        WHERE o.status IN :statuses
          AND o.deletedAt IS NULL
    """)
    List<MapOccurrenceDTO> findMapDataByStatusIn(@Param("statuses") Collection<OccurrenceStatus> statuses);
}
