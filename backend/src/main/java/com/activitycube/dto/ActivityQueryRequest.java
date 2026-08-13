package com.activitycube.dto;

import lombok.Data;

@Data
public class ActivityQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private String keyword;
    private String campus;
    private String status;
    private String activityMode;
}
