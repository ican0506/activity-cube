package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.FeedbackMediaRequest;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.FeedbackMedia;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.FeedbackMediaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackMediaService {
    private final FeedbackMediaMapper feedbackMediaMapper;
    private final FeedbackMapper feedbackMapper;

    public List<FeedbackMedia> listByFeedback(Long feedbackId) {
        return feedbackMediaMapper.selectList(new LambdaQueryWrapper<FeedbackMedia>()
                .eq(FeedbackMedia::getFeedbackId, feedbackId)
                .orderByAsc(FeedbackMedia::getId));
    }

    public List<FeedbackMedia> saveAll(Long feedbackId, FeedbackMediaRequest request) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        feedbackMediaMapper.delete(new LambdaQueryWrapper<FeedbackMedia>().eq(FeedbackMedia::getFeedbackId, feedbackId));
        List<FeedbackMedia> items = request.getItems() == null ? List.of() : request.getItems().stream()
                .map(item -> toEntity(feedback, item))
                .toList();
        items.forEach(feedbackMediaMapper::insert);
        return items;
    }

    private FeedbackMedia toEntity(Feedback feedback, FeedbackMediaRequest.Item item) {
        validate(item.getMediaType(), item.getUrl());
        FeedbackMedia media = new FeedbackMedia();
        media.setFeedbackId(feedback.getId());
        media.setActivityId(feedback.getActivityId());
        media.setUserId(feedback.getUserId());
        media.setMediaType(item.getMediaType());
        media.setUrl(item.getUrl());
        media.setFileName(item.getFileName());
        media.setOriginalName(item.getOriginalName());
        media.setSize(item.getSize());
        media.setCreateTime(LocalDateTime.now());
        return media;
    }

    private void validate(String mediaType, String url) {
        if (!"image".equals(mediaType) && !"video".equals(mediaType)) {
            throw new BusinessException("媒体类型不正确");
        }
        if (url == null || url.isBlank()) {
            throw new BusinessException("媒体地址不能为空");
        }
    }
}
