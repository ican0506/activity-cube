package com.activitycube.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackView {
    private Long id;
    private Long userId;
    private String realName;
    private String feedbackType;
    private Integer score;
    private String content;
    private String suggestion;
    private String handleStatus;
    private Boolean anonymous;
    private LocalDateTime createdAt;
}
