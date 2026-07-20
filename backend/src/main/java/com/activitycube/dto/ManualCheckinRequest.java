package com.activitycube.dto;

import lombok.Data;

@Data
public class ManualCheckinRequest {
    private Long userId;
    private String studentNo;
    private String remark;
}
