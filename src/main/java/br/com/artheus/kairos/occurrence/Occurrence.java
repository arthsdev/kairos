package br.com.artheus.kairos.occurrence;

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

    public void delete() {
        if (this.deletedAt != null) {
           throw new BusinessException("You can't delete this occurrence.");
        }
        this.deletedAt = LocalDateTime.now();
    }

    public void verify() {
        if (this.status != OccurrenceStatus.PENDING) {
            throw new BusinessException("Only pending occurrences can be verified");
        }
        this.status = OccurrenceStatus.VERIFIED;
    }

    public void resolve() {
        if (this.status != OccurrenceStatus.VERIFIED) {
            throw new BusinessException("Only verified occurrences can be resolved");
        }
        this.status = OccurrenceStatus.RESOLVED;
    }

    private void validateEditable() {
        if (this.status == OccurrenceStatus.VERIFIED || this.status == OccurrenceStatus.RESOLVED) {
            throw new BusinessException("Only pending occurrences can be edited");
        }
    }

    public void changeTitle(String title) {
        validateEditable();
        this.title = title;
    }
    public void changeDescription(String description) {
        validateEditable();
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = OccurrenceStatus.PENDING;
    }
}