package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_receiver")
public class NoticeReceiver {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noticeId;
    private Long receiverId;
    private Integer readStatus;
    private LocalDateTime readTime;
    @TableField("create_time")
    private LocalDateTime createdAt;
}
