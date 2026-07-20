package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice")
public class Notice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long senderId;
    private String senderName;
    private String title;
    private String content;
    private String noticeType;
    private String targetType;
    @TableField("create_time")
    private LocalDateTime createdAt;
}
