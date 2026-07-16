package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRoleRequest {
    @NotBlank
    private String role;
}
