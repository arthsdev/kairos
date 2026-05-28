package br.com.artheus.kairos.plan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Column(nullable = false)
    private int cityLimit;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;


    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void upgradeToPremium() {
        this.planType = PlanType.PREMIUM;
        this.expiresAt = LocalDateTime.now().plusDays(30);
        this.updatedAt = LocalDateTime.now();
        cityLimit = 5;
    }

    public void downgradeToFree() {
        this.planType = PlanType.FREE;
        this.cityLimit = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasReachedCityLimit(long currentCount) {
        return currentCount >= this.cityLimit;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        planType = PlanType.TRIAL;
        cityLimit = 5;
        expiresAt = LocalDateTime.now().plusDays(7);
    }
}
