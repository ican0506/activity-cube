package com.activitycube.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentTodoItem {
    private String type;
    private String title;
    private String description;
    private String targetPath;
    private Long activityId;
    private LocalDateTime dueTime;
    private String priority;
}
