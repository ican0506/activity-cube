package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    @NotBlank
    private String noticeType;
    @NotBlank
    private String targetType;
}
