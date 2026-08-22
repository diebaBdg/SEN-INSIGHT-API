package com.seninsight.backend.business.user.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionDTO {
    private Long id;
    private String code;
    private String libelle;
    private String description;
    private String module;
    private String categorie;
    private Boolean actif;
    private LocalDateTime dateCreation;
}
