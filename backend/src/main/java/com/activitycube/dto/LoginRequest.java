package com.activitycube.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String studentNo;
    private String workNo;
    private String password;

    public String resolveIdentifier() {
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        if (studentNo != null && !studentNo.isBlank()) {
            return studentNo.trim();
        }
        if (workNo != null && !workNo.isBlank()) {
            return workNo.trim();
        }
        return "";
    }
}
