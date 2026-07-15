package com.activitycube.vo;

import lombok.Data;

import java.util.Map;

@Data
public class FeedbackStats {
    private Long activityId;
    private Long feedbackCount;
    private Double averageScore;
    private Map<Integer, Long> scoreDistribution;
}
