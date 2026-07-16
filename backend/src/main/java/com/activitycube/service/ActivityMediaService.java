package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ActivityMediaRequest;
import com.activitycube.entity.ActivityMedia;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMediaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityMediaService {
    private final ActivityMediaMapper activityMediaMapper;
    private final ActivityService activityService;

    public List<ActivityMedia> listByActivity(Long activityId) {
        return activityMediaMapper.selectList(new LambdaQueryWrapper<ActivityMedia>()
                .eq(ActivityMedia::getActivityId, activityId)
                .orderByAsc(ActivityMedia::getSortOrder)
                .orderByAsc(ActivityMedia::getId));
    }

    public List<ActivityMedia> saveAll(Long activityId, ActivityMediaRequest request, User user) {
        activityService.requireManageableActivity(activityId, user);
        activityMediaMapper.delete(new LambdaQueryWrapper<ActivityMedia>().eq(ActivityMedia::getActivityId, activityId));
        List<ActivityMedia> items = request.getItems() == null ? List.of() : request.getItems().stream()
                .map(item -> toEntity(activityId, item))
                .toList();
        items.forEach(activityMediaMapper::insert);
        return items;
    }

    public void delete(Long mediaId, User user) {
        ActivityMedia media = activityMediaMapper.selectById(mediaId);
        if (media == null) {
            throw new BusinessException("媒体不存在");
        }
        activityService.requireManageableActivity(media.getActivityId(), user);
        activityMediaMapper.deleteById(mediaId);
    }

    private ActivityMedia toEntity(Long activityId, ActivityMediaRequest.Item item) {
        validate(item.getMediaType(), item.getUsageType(), item.getUrl());
        ActivityMedia media = new ActivityMedia();
        media.setActivityId(activityId);
        media.setMediaType(item.getMediaType());
        media.setUsageType(item.getUsageType());
        media.setUrl(item.getUrl());
        media.setFileName(item.getFileName());
        media.setOriginalName(item.getOriginalName());
        media.setSize(item.getSize());
        media.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
        media.setCreateTime(LocalDateTime.now());
        return media;
    }

    private void validate(String mediaType, String usageType, String url) {
        if (!"image".equals(mediaType) && !"video".equals(mediaType)) {
            throw new BusinessException("媒体类型不正确");
        }
        if (!"cover".equals(usageType) && !"gallery".equals(usageType) && !"video".equals(usageType)) {
            throw new BusinessException("媒体用途不正确");
        }
        if (url == null || url.isBlank()) {
            throw new BusinessException("媒体地址不能为空");
        }
    }
}
