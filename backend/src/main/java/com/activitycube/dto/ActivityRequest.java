package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    private String coverUrl;
    private String activityMode;
    private String activityCategory;
    @NotBlank
    private String campus;
    @NotBlank
    private String location;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private LocalDateTime registerStartTime;
    @NotNull
    private LocalDateTime registerEndTime;
    @NotNull
    private LocalDateTime checkinStartTime;
    @NotNull
    private LocalDateTime checkinEndTime;
    private Integer maxParticipants;
    @NotNull
    private Boolean allowCrossCampus;
    private Boolean rewardEnabled;
    private String rewardType;
    private BigDecimal rewardHours;
    private Integer rewardPoints;
    private String rewardDescription;
    @NotBlank
    private String status;
}
