package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.FeedbackRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.FeedbackStats;
import com.activitycube.vo.FeedbackView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;
    private final RegistrationService registrationService;
    private final ActivityService activityService;
    private final UserMapper userMapper;

    public Feedback submit(Long activityId, FeedbackRequest request, User user) {
        Activity activity = activityService.requireActivity(activityId);
        if (!ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("活动结束后才可以提交反馈");
        }
        registrationService.requireRegistration(activityId, user.getId());
        if (feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .eq(Feedback::getUserId, user.getId())) > 0) {
            throw new BusinessException("你已提交过反馈");
        }
        Feedback feedback = new Feedback();
        feedback.setActivityId(activityId);
        feedback.setUserId(user.getId());
        feedback.setScore(resolveScore(request));
        feedback.setContent(request.getContent());
        feedback.setSuggestion(request.getSuggestion());
        feedback.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public List<FeedbackView> listByActivity(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .orderByDesc(Feedback::getCreatedAt))
                .stream()
                .map(this::toView)
                .toList();
    }

    public FeedbackStats stats(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        List<Feedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId));
        Map<Integer, Long> grouped = feedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getScore, Collectors.counting()));
        Map<Integer, Long> distribution = IntStream.rangeClosed(1, 5)
                .boxed()
                .sorted((left, right) -> Integer.compare(right, left))
                .collect(Collectors.toMap(
                        score -> score,
                        score -> grouped.getOrDefault(score, 0L),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        FeedbackStats stats = new FeedbackStats();
        stats.setActivityId(activityId);
        stats.setFeedbackCount((long) feedbacks.size());
        stats.setAverageScore(feedbacks.isEmpty() ? 0 : feedbacks.stream().mapToInt(Feedback::getScore).average().orElse(0));
        stats.setScoreDistribution(distribution);
        return stats;
    }

    private Integer resolveScore(FeedbackRequest request) {
        Integer score = request.getScore() != null ? request.getScore() : request.getRating();
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException("满意度评分必须在 1 到 5 之间");
        }
        return score;
    }

    private FeedbackView toView(Feedback feedback) {
        FeedbackView view = new FeedbackView();
        view.setId(feedback.getId());
        view.setUserId(feedback.getUserId());
        view.setScore(feedback.getScore());
        view.setContent(feedback.getContent());
        view.setSuggestion(feedback.getSuggestion());
        view.setAnonymous(Boolean.TRUE.equals(feedback.getAnonymous()));
        view.setCreatedAt(feedback.getCreatedAt());
        if (Boolean.TRUE.equals(feedback.getAnonymous())) {
            view.setRealName("匿名同学");
        } else {
            User user = userMapper.selectById(feedback.getUserId());
            view.setRealName(user == null ? "未知用户" : user.getRealName());
        }
        return view;
    }
}
