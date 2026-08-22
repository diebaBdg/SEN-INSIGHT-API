package com.seninsight.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String role;
}
