package com.activitycube.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DrawRequest {
    @NotBlank
    private String source;
    @NotNull
    @Min(1)
    private Integer count;
}
