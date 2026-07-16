package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrganizerCreateRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String realName;
    private String studentNo;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank
    @Pattern(regexp = "龙子湖校区|文化路校区|许昌校区", message = "只能选择龙子湖校区、文化路校区或许昌校区")
    private String campus;
    private String college;
}
