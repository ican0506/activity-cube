package com.activitycube.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivitySummaryContext {
    private Long activityId;
    private String activityName;
    private String activityCategory;
    private String campus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean phaseSummary;
    private Long registrationCount;
    private Long checkinCount;
    private Long absentCount;
    private Double checkinRate;
    private Long feedbackCount;
    private Double averageScore;
    private List<String> feedbackTexts;
}
