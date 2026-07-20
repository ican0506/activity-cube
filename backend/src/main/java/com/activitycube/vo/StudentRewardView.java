package com.activitycube.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentRewardView {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private String activityCategory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String rewardType;
    private BigDecimal rewardHours;
    private Integer rewardPoints;
    private String rewardDescription;
    private LocalDateTime issuedTime;
    private String status;
    private String remark;
}
