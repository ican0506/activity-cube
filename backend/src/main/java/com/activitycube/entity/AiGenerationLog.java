package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_generation_log")
public class AiGenerationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String generationType;
    private Long activityId;
    private Long operatorId;
    private String modelName;
    private String inputSummary;
    private String outputContent;
    private Integer success;
    private String errorMessage;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
