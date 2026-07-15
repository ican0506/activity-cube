package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
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
    private Integer maxParticipants;
    @NotNull
    private Boolean allowCrossCampus;
    @NotBlank
    private String status;
}
