package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String studentNo;
    @NotBlank
    private String college;
    @NotBlank
    private String majorClass;
    private String phone;
    @NotBlank
    private String campus;
    private String remark;
}
