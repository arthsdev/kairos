package br.com.artheus.kairos.climate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "climate_data")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClimateData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false)
    private double humidity;

    @Column(nullable = false)
    private double rainVolume;

    @Column(nullable = false)
    private double windSpeed;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private MonitoredCity city;

}
