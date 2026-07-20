package com.activitycube.dto;

import lombok.Data;

@Data
public class StudentProfileUpdateRequest {
    private String avatarUrl;
    private String phone;
    private String bio;
}
