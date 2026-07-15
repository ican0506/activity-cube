package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.FeedbackRequest;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.FeedbackMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;
    private final RegistrationService registrationService;
    private final ActivityService activityService;

    public Feedback submit(Long activityId, FeedbackRequest request, User user) {
        activityService.requireActivity(activityId);
        registrationService.requireRegistration(activityId, user.getId());
        if (feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .eq(Feedback::getUserId, user.getId())) > 0) {
            throw new BusinessException("不能重复提交反馈");
        }
        Feedback feedback = new Feedback();
        feedback.setActivityId(activityId);
        feedback.setUserId(user.getId());
        feedback.setRating(request.getRating());
        feedback.setContent(request.getContent());
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public List<Feedback> listByActivity(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .orderByDesc(Feedback::getCreatedAt));
    }
}
