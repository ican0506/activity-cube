package com.activitycube.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeMessage {
    private Long id;
    private Long noticeId;
    private Long activityId;
    private String activityTitle;
    private String senderName;
    private String title;
    private String content;
    private String summary;
    private String noticeType;
    private String targetType;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
}
