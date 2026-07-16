package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserResetPasswordRequest {
    @NotBlank
    private String password;
}
