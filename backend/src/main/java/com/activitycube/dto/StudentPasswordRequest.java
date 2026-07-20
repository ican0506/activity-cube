package com.activitycube.dto;

import lombok.Data;

@Data
public class StudentPasswordRequest {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
