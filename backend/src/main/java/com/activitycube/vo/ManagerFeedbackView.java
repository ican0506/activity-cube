package com.activitycube.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ManagerFeedbackView {
    private Long id;
    private Long activityId;
    private String activityName;
    private Long userId;
    private String studentName;
    private String feedbackType;
    private String content;
    private Integer score;
    private LocalDateTime createdAt;
}
