package com.activitycube.service;

import com.activitycube.dto.ActivityMediaRequest;
import com.activitycube.dto.FeedbackMediaRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.ActivityMedia;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.FeedbackMedia;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMediaMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.FeedbackMediaMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaServiceTest {
    private final ActivityMediaMapper activityMediaMapper = mock(ActivityMediaMapper.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final FeedbackMediaMapper feedbackMediaMapper = mock(FeedbackMediaMapper.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);

    @Test
    void savesActivityMediaWithSortOrder() {
        ActivityMediaService service = new ActivityMediaService(activityMediaMapper, activityService);
        User organizer = manager();
        Activity activity = new Activity();
        activity.setId(1L);
        when(activityService.requireManageableActivity(1L, organizer)).thenReturn(activity);

        ActivityMediaRequest request = new ActivityMediaRequest();
        request.setItems(List.of(mediaItem("/uploads/2026/07/a.jpg", "image", "gallery", 1)));

        List<ActivityMedia> saved = service.saveAll(1L, request, organizer);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getActivityId()).isEqualTo(1L);
        assertThat(saved.get(0).getSortOrder()).isEqualTo(1);
        verify(activityMediaMapper).delete(any());
        verify(activityMediaMapper).insert(saved.get(0));
    }

    @Test
    void savesFeedbackMediaWithFeedbackOwnerInfo() {
        FeedbackMediaService service = new FeedbackMediaService(feedbackMediaMapper, feedbackMapper);
        Feedback feedback = new Feedback();
        feedback.setId(9L);
        feedback.setActivityId(1L);
        feedback.setUserId(3L);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);

        FeedbackMediaRequest request = new FeedbackMediaRequest();
        request.setItems(List.of(feedbackItem("/uploads/2026/07/v.mp4", "video")));

        List<FeedbackMedia> saved = service.saveAll(9L, request);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getFeedbackId()).isEqualTo(9L);
        assertThat(saved.get(0).getActivityId()).isEqualTo(1L);
        assertThat(saved.get(0).getUserId()).isEqualTo(3L);
        verify(feedbackMediaMapper).delete(any());
        verify(feedbackMediaMapper).insert(saved.get(0));
    }

    private ActivityMediaRequest.Item mediaItem(String url, String mediaType, String usageType, int sortOrder) {
        ActivityMediaRequest.Item item = new ActivityMediaRequest.Item();
        item.setUrl(url);
        item.setMediaType(mediaType);
        item.setUsageType(usageType);
        item.setFileName("a.jpg");
        item.setOriginalName("a.jpg");
        item.setSize(10L);
        item.setSortOrder(sortOrder);
        return item;
    }

    private FeedbackMediaRequest.Item feedbackItem(String url, String mediaType) {
        FeedbackMediaRequest.Item item = new FeedbackMediaRequest.Item();
        item.setUrl(url);
        item.setMediaType(mediaType);
        item.setFileName("v.mp4");
        item.setOriginalName("v.mp4");
        item.setSize(10L);
        return item;
    }

    private User manager() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        return user;
    }
}
