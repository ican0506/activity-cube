package com.activitycube.vo;

import com.activitycube.entity.Activity;
import lombok.Data;

@Data
public class ActivityDetail {
    private Activity activity;
    private Long registrationCount;
    private Long checkinCount;
    private Boolean registered;
    private Boolean checkedIn;
}
