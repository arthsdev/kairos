package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "occurrences")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "city_id", nullable = false)
    private String cityId;

    public void verify() {
        this.ensureNotDeleted();
        if (this.status != OccurrenceStatus.PENDING) {
            throw new BusinessException("Only pending occurrences can be verified");
        }
        this.status = OccurrenceStatus.VERIFIED;
    }

    public void resolve() {
        this.ensureNotDeleted();
        if (this.status != OccurrenceStatus.VERIFIED) {
            throw new BusinessException("Only verified occurrences can be resolved");
        }
        this.status = OccurrenceStatus.RESOLVED;
    }

    public void changeTitle(String title) {
        this.ensureNotDeleted();
        this.ensureNotFinalizedForUpdate();
        this.title = title;
    }

    public void changeDescription(String description) {
        this.ensureNotDeleted();
        this.ensureNotFinalizedForUpdate();
        this.description = description;
    }

    public void delete() {
        if (this.isDeleted()) {
            throw new BusinessException("You can't delete this occurrence.");
        }
        ensureNotFinalizedForDeletion();
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isFinalized() {
        return this.status == OccurrenceStatus.VERIFIED || this.status == OccurrenceStatus.RESOLVED;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (this.isDeleted()) {
            throw new BusinessException("Cannot perform this action on a deleted occurrence.");
        }
    }

    private void ensureNotFinalizedForUpdate() {
        if (this.isFinalized()) {
            throw new BusinessException("Cannot update the occurrence because it has already been finalized.");
        }
    }

    private void ensureNotFinalizedForDeletion() {
        if (this.isFinalized()) {
            throw new BusinessException("Cannot delete the occurrence because it has already been finalized.");
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = OccurrenceStatus.PENDING;
    }
}