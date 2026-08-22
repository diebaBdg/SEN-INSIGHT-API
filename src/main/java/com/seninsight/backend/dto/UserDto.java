package com.seninsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private String organization;
    private String phone;
    private Boolean isActive;
    private Boolean emailVerified;
    private Instant createdAt;
}
