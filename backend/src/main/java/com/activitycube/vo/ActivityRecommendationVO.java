package com.activitycube.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ActivityRecommendationVO {
    private Long activityId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String campus;
    private String activityMode;
    private String activityCategory;
    private Integer recommendScore;
    private String aiReason;
    private Map<String, Integer> scoreDetails;
    private Long registrationCount;
    private Integer maxParticipants;
}
