package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_activity_reward")
public class StudentActivityReward {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long studentId;
    private String studentNo;
    private String activityCategory;
    private String rewardType;
    private BigDecimal rewardHours;
    private Integer rewardPoints;
    private String rewardDescription;
    private Long issuedBy;
    private String issuedByName;
    private LocalDateTime issuedTime;
    private String status;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createdAt;
}
