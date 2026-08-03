package com.activitycube.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivitySummaryResult {
    private String overview;
    private String dataAnalysis;
    private List<String> highlights;
    private String feedbackSummary;
    private List<String> problems;
    private List<String> suggestions;
    private String nextAction;
    private Boolean phaseSummary;
    private LocalDateTime generatedAt;
}
