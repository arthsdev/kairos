package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitored_cities")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class MonitoredCity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void changeName(String name) {
        if (!this.active) {
            throw new BusinessException("Cannot change name of inactive city");
        }
        this.name = name;
    }

    public void changeState(String newState) {
        if (!this.active) {
            throw new BusinessException("Cannot change state of inactive city");
        }

        if (this.state.equals(newState)) {
            throw new BusinessException("The city is already in this state");
        }

        this.state = newState;
    }

    public void inactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }
}
