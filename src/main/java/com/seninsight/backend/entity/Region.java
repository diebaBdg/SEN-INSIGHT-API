package com.seninsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Region {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "chief_town")
    private String chiefTown;

    @Column(name = "population")
    private Long population;

    @Column(name = "area_km2")
    private Double areaKm2;

    @Column(name = "population_density")
    private Double populationDensity;

    private Double latitude;
    private Double longitude;
}
