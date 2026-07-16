package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback_media")
public class FeedbackMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long feedbackId;
    private Long activityId;
    private Long userId;
    private String mediaType;
    private String url;
    private String fileName;
    private String originalName;
    private Long size;
    private LocalDateTime createTime;
}
