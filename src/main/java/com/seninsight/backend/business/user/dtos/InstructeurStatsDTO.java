package com.seninsight.backend.business.user.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructeurStatsDTO {
    private UUID instructeurId;
    private String fullName;
    private Integer totalAssigned;
    private Integer totalApproved;
    private Integer totalRejected;
    private Double tauxApprobation;
    private String performance;
}