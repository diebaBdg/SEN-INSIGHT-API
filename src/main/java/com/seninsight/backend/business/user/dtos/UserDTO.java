package com.seninsight.backend.business.user.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String adresse;
    private UserStatus status;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Set<RoleDTO> roles;

    private Integer totalAssigned;
    private Integer totalApproved;
    private Integer totalRejected;
}