package com.activitycube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupRequest {
    @NotBlank
    private String source;
    @NotBlank
    private String mode;
    private Integer groupCount;
    private Integer groupSize;
}
