package com.activitycube.vo;

import lombok.Data;

import java.util.List;

@Data
public class ActivityStats {
    private Long activityId;
    private Long registrationCount;
    private Long checkinCount;
    private Long absenceCount;
    private Double checkinRate;
    private Long feedbackCount;
    private Double averageRating;
    private List<CampusStat> campusStats;
}
