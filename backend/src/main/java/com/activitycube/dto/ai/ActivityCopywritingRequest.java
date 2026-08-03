package com.activitycube.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCopywritingRequest {
    @NotBlank(message = "不能为空")
    private String activityName;
    private String activityType;
    private String campus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String targetAudience;
    private String highlights;
    private String registrationRequirements;
    private String tone;
}
