package com.activitycube.vo;

import lombok.Data;

@Data
public class CheckinCodeResolveResult {
    private Long activityId;
    private String activityName;
    private String activityMode;
    private String checkinCode;
}
