package com.activitycube.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String feedbackType;
    private Integer score;
    private String content;
    private String suggestion;
    private String handleStatus;
    private Boolean anonymous;
    @TableField("create_time")
    private LocalDateTime createdAt;

    public Integer getRating() {
        return score;
    }

    public void setRating(Integer rating) {
        this.score = rating;
    }
}
