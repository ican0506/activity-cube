package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterUserRequest {
    private String username;
    private String role;
    @NotBlank
    private String password;
    @NotBlank
    private String realName;
    @NotBlank
    @Pattern(regexp = "^\\d{10}$", message = "学号必须为10位数字")
    private String studentNo;
    @NotBlank
    @Pattern(regexp = "龙子湖校区|文化路校区|许昌校区", message = "只能选择龙子湖校区、文化路校区或许昌校区")
    private String campus;
    @NotBlank
    private String college;
    private String majorCode;
    @NotBlank
    private String majorName;
    @NotBlank
    private String className;
    private String majorClass;
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
