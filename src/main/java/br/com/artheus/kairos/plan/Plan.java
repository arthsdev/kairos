package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.exception.BusinessException;
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

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;


    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void upgradeToPremium(String stripeCustomerId, String stripeSubscriptionId) {
        if (this.planType == PlanType.PREMIUM) {
            throw new BusinessException("Plan is already Premium");
        }
        this.planType = PlanType.PREMIUM;
        this.expiresAt = null;
        this.cityLimit = 5;
        this.stripeCustomerId = stripeCustomerId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.updatedAt = LocalDateTime.now();
    }

    public void downgradeToFree() {
        this.planType = PlanType.FREE;
        this.cityLimit = 1;
        this.expiresAt = null;
        this.stripeSubscriptionId = null;
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