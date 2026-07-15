package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.FeedbackRequest;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.vo.FeedbackView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final FeedbackService feedbackService = new FeedbackService(feedbackMapper, registrationService, activityService, userMapper);

    @Test
    void storesSuggestionAndAnonymousFlagWhenSubmittingFeedback() {
        FeedbackRequest request = new FeedbackRequest();
        request.setScore(5);
        request.setContent("活动体验很好");
        request.setSuggestion("希望增加互动");
        request.setAnonymous(true);
        User user = new User();
        user.setId(3L);

        Feedback feedback = feedbackService.submit(1L, request, user);

        assertThat(feedback.getScore()).isEqualTo(5);
        assertThat(feedback.getContent()).isEqualTo("活动体验很好");
        assertThat(feedback.getSuggestion()).isEqualTo("希望增加互动");
        assertThat(feedback.getAnonymous()).isTrue();
        verify(feedbackMapper).insert(feedback);
    }

    @Test
    void rejectsDuplicateFeedbackFromSameUser() {
        FeedbackRequest request = new FeedbackRequest();
        request.setScore(4);
        User user = new User();
        user.setId(3L);
        when(feedbackMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> feedbackService.submit(1L, request, user))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能重复提交反馈");
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    void hidesRealNameForAnonymousFeedbackInAdminList() {
        Feedback anonymous = feedback(3L, true);
        Feedback named = feedback(4L, false);
        when(feedbackMapper.selectList(any())).thenReturn(List.of(anonymous, named));
        User user = new User();
        user.setId(4L);
        user.setRealName("李四");
        when(userMapper.selectById(4L)).thenReturn(user);

        List<FeedbackView> views = feedbackService.listByActivity(1L, manager());

        assertThat(views).hasSize(2);
        assertThat(views.get(0).getRealName()).isEqualTo("匿名用户");
        assertThat(views.get(1).getRealName()).isEqualTo("李四");
        verify(userMapper, never()).selectById(3L);
    }

    private Feedback feedback(Long userId, boolean anonymous) {
        Feedback feedback = new Feedback();
        feedback.setActivityId(1L);
        feedback.setUserId(userId);
        feedback.setScore(5);
        feedback.setContent("体验内容");
        feedback.setSuggestion("建议内容");
        feedback.setAnonymous(anonymous);
        return feedback;
    }

    private User manager() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        return user;
    }
}
