package com.activitycube.vo;

import lombok.Data;

import java.util.Map;

@Data
public class FeedbackStats {
    private Long activityId;
    private Long feedbackCount;
    private Long suggestionCount;
    private Long issueCount;
    private Long evaluationCount;
    private Double averageScore;
    private Map<Integer, Long> scoreDistribution;
}
