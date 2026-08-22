package com.seninsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "indicator_series")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IndicatorSeries {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "indicator_code", nullable = false)
    private String indicatorCode;

    @Column(name = "region_id")
    private String regionId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private Boolean national = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
