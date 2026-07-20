package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("registration")
public class Registration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String name;
    private String studentNo;
    private String college;
    private String majorClass;
    private String phone;
    private String campus;
    private String remark;
    @TableField(exist = false)
    private String activityTitle;
    @TableField(exist = false)
    private String activityStatus;
    @TableField(exist = false)
    private Boolean canCancel;
    @TableField(exist = false)
    private String cancelReason;
    @TableField("create_time")
    private LocalDateTime createdAt;
}
