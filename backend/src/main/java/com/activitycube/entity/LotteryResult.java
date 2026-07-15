package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lottery_result")
public class LotteryResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private Long registrationId;
    private String realName;
    private String studentNo;
    private String college;
    @TableField("class_name")
    private String className;
    private String phone;
    private String campus;
    private String source;
    private Integer roundNo;
    @TableField("create_time")
    private LocalDateTime createTime;
}
