package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String realName;
    @NotBlank
    private String studentNo;
    @NotBlank
    @Pattern(regexp = "龙子湖校区|文化路校区|许昌校区", message = "只能选择龙子湖校区、文化路校区或许昌校区")
    private String campus;
    @NotBlank
    private String college;
    @NotBlank
    private String majorClass;
    private String phone;
}
