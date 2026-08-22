package com.seninsight.backend.business.user.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleDTO {
    private long id;
    private String code;
    private String libelle;
    private String description;
    private Integer niveauAutorisation;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private Integer permissionCount;
    private List<PermissionDTO> permissions;
    private List<String> permissionCodes;
}
