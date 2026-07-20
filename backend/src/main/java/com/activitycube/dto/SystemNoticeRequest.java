package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemNoticeRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String targetType;
}
