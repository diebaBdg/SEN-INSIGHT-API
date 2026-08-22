package com.seninsight.backend.business.user.dtos;

import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsRequest {
    private List<Long> permissionIds;
    private List<String> permissionCodes;
}
