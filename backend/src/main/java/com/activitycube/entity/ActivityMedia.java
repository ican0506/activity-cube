package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_media")
public class ActivityMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private String mediaType;
    private String usageType;
    private String url;
    private String fileName;
    private String originalName;
    private Long size;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
