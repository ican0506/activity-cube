package com.activitycube.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentProfileSummary {
    private Long registrationCount = 0L;
    private Long checkinCount = 0L;
    private Long pendingFeedbackCount = 0L;
    private BigDecimal rewardHours = BigDecimal.ZERO;
    private Integer rewardPoints = 0;
    private Long unreadMessageCount = 0L;
}
