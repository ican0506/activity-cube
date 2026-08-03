package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_recommendation_log")
public class ActivityRecommendationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Integer recommendScore;
    private String ruleDetail;
    private String aiReason;
    private String modelName;
    private Integer success;
    private String errorMessage;
    @TableField("create_time")
    private LocalDateTime createdAt;
}
