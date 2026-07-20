package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String activityMode;
    private String checkinCode;
    private String rejectReason;
    private String campus;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registerStartTime;
    private LocalDateTime registerEndTime;
    private LocalDateTime checkinStartTime;
    private LocalDateTime checkinEndTime;
    private Integer maxParticipants;
    private Boolean allowCrossCampus;
    private String status;
    @TableField(exist = false)
    private String reviewStatus;
    private Long creatorId;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
}
