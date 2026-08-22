package com.seninsight.backend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String organization;
    private String phone;
}
