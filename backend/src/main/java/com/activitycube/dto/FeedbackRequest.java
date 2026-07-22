package com.activitycube.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FeedbackRequest {
    private String feedbackType;
    @Min(1)
    @Max(5)
    private Integer score;
    @Min(1)
    @Max(5)
    private Integer rating;
    private String content;
    private String suggestion;
    private Boolean anonymous;
}
