package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.FeedbackRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
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
    public static final String TYPE_SUGGESTION = "suggestion";
    public static final String TYPE_ISSUE = "issue";
    public static final String TYPE_EVALUATION = "evaluation";

    private final FeedbackMapper feedbackMapper;
    private final RegistrationService registrationService;
    private final ActivityService activityService;
    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;

    public Feedback submit(Long activityId, FeedbackRequest request, User user) {
        Activity activity = activityService.requireActivity(activityId);
        String feedbackType = resolveFeedbackType(request);
        validateActivityAllowsFeedback(activity);
        if (TYPE_EVALUATION.equals(feedbackType)) {
            validateEvaluationAllowed(activityId, activity, user);
        }
        if (TYPE_EVALUATION.equals(feedbackType) && feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getActivityId, activityId)
                .eq(Feedback::getUserId, user.getId())
                .eq(Feedback::getFeedbackType, TYPE_EVALUATION)) > 0) {
            throw new BusinessException("你已提交过活动评价");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException("反馈内容不能为空");
        }
        Feedback feedback = new Feedback();
        feedback.setActivityId(activityId);
        feedback.setUserId(user.getId());
        feedback.setFeedbackType(feedbackType);
        feedback.setScore(TYPE_EVALUATION.equals(feedbackType) ? resolveScore(request) : null);
        feedback.setContent(content);
        feedback.setSuggestion(request.getSuggestion());
        feedback.setHandleStatus("pending");
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
                .filter(feedback -> TYPE_EVALUATION.equals(resolveFeedbackType(feedback.getFeedbackType())))
                .filter(feedback -> feedback.getScore() != null)
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
        stats.setSuggestionCount(countType(feedbacks, TYPE_SUGGESTION));
        stats.setIssueCount(countType(feedbacks, TYPE_ISSUE));
        stats.setEvaluationCount(countType(feedbacks, TYPE_EVALUATION));
        stats.setAverageScore(stats.getEvaluationCount() == 0
                ? 0
                : feedbacks.stream()
                        .filter(feedback -> TYPE_EVALUATION.equals(resolveFeedbackType(feedback.getFeedbackType())))
                        .filter(feedback -> feedback.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .average()
                        .orElse(0));
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
        view.setFeedbackType(resolveFeedbackType(feedback.getFeedbackType()));
        view.setScore(feedback.getScore());
        view.setContent(feedback.getContent());
        view.setSuggestion(feedback.getSuggestion());
        view.setHandleStatus(feedback.getHandleStatus() == null ? "pending" : feedback.getHandleStatus());
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

    private String resolveFeedbackType(FeedbackRequest request) {
        return resolveFeedbackType(request.getFeedbackType());
    }

    private String resolveFeedbackType(String feedbackType) {
        if (TYPE_SUGGESTION.equals(feedbackType)) {
            return TYPE_SUGGESTION;
        }
        if (TYPE_ISSUE.equals(feedbackType)) {
            return TYPE_ISSUE;
        }
        return TYPE_EVALUATION;
    }

    private void validateActivityAllowsFeedback(Activity activity) {
        String workflowStatus = activity.getReviewStatus() == null ? activity.getStatus() : activity.getReviewStatus();
        if (ActivityStatusUtil.DRAFT.equals(workflowStatus)
                || ActivityStatusUtil.PENDING_REVIEW.equals(workflowStatus)
                || ActivityStatusUtil.REJECTED.equals(workflowStatus)) {
            throw new BusinessException("活动发布后才可以提交反馈建议");
        }
        if (ActivityStatusUtil.CANCELLED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("当前活动已取消，不能提交反馈");
        }
    }

    private void validateEvaluationAllowed(Long activityId, Activity activity, User user) {
        registrationService.requireRegistration(activityId, user.getId());
        boolean ended = ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity));
        boolean checkedIn = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, user.getId())) > 0;
        if (!ended && !checkedIn) {
            throw new BusinessException("签到后或活动结束后才可以提交活动评价");
        }
    }

    private Long countType(List<Feedback> feedbacks, String feedbackType) {
        return feedbacks.stream()
                .filter(feedback -> feedbackType.equals(resolveFeedbackType(feedback.getFeedbackType())))
                .count();
    }
}
