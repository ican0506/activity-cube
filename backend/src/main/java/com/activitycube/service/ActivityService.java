package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ActivityRequest;
import com.activitycube.dto.RejectActivityRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.UserContext;
import com.activitycube.vo.ActivityDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {
    public static final String MODE_ONLINE = "online";
    public static final String MODE_OFFLINE = "offline";

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

    public List<Activity> list(String keyword, String campus, String status) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .orderByDesc(Activity::getCreatedAt);
        if (!isManager(UserContext.get().orElse(null))) {
            wrapper.eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Activity::getTitle, keyword);
        }
        if (StringUtils.hasText(campus) && !"全部".equals(campus)) {
            wrapper.eq(Activity::getCampus, campus);
        }
        return activityMapper.selectList(wrapper).stream()
                .peek(this::applyResponseDefaults)
                .filter(activity -> !StringUtils.hasText(status)
                        || "全部".equals(status)
                        || status.equals(activity.getStatus()))
                .toList();
    }

    public ActivityDetail detail(Long id) {
        Activity activity = requireActivity(id);
        if (!canViewActivity(activity, UserContext.get().orElse(null))) {
            throw new BusinessException("活动尚未发布");
        }
        Long registrationCount = countRegistrations(id);
        Long checkinCount = countCheckins(id);
        ActivityDetail detail = new ActivityDetail();
        detail.setActivity(activity);
        detail.setRegistrationCount(registrationCount);
        detail.setCheckinCount(checkinCount);
        UserContext.get().ifPresent(user -> {
            detail.setRegistered(hasRegistration(id, user.getId()));
            detail.setCheckedIn(hasCheckin(id, user.getId()));
        });
        return detail;
    }

    public Activity create(ActivityRequest request, User creator) {
        AuthUtil.requireOrganizerOrAdmin(creator);
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activity.setStatus(ActivityStatusUtil.DRAFT);
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        activity.setCreatorId(creator.getId());
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity update(Long id, ActivityRequest request, User user) {
        Activity activity = requireManageableActivity(id, user);
        if (ActivityStatusUtil.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException("活动已取消，不能继续编辑");
        }
        if (ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())) {
            throw new BusinessException("活动正在审核中，暂时不能编辑");
        }
        String workflowStatus = activity.getStatus();
        BeanUtils.copyProperties(request, activity);
        activity.setStatus(workflowStatus);
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        applyResponseDefaults(activity);
        return activity;
    }

    public void delete(Long id, User user) {
        requireManageableActivity(id, user);
        activityMapper.deleteById(id);
    }

    public Activity submitReview(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        if (!ActivityStatusUtil.DRAFT.equals(activity.getStatus()) && !ActivityStatusUtil.REJECTED.equals(activity.getStatus())) {
            throw new BusinessException("当前活动不能提交审核");
        }
        activity.setStatus(ActivityStatusUtil.PENDING_REVIEW);
        activity.setRejectReason(null);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "submit_activity_review", "activity", id, "提交活动审核：" + activity.getTitle());
        applyResponseDefaults(activity);
        return activity;
    }

    public List<Activity> pendingReviews(User user) {
        AuthUtil.requireAdmin(user);
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getStatus, ActivityStatusUtil.PENDING_REVIEW)
                        .orderByAsc(Activity::getUpdatedAt))
                .stream()
                .peek(this::applyResponseDefaults)
                .toList();
    }

    public Activity approveReview(Long id, User user) {
        AuthUtil.requireAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())) {
            throw new BusinessException("该活动不处于待审核状态");
        }
        activity.setStatus(ActivityStatusUtil.PUBLISHED);
        activity.setRejectReason(null);
        ensureCheckinCodeIfPublished(activity);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "approve_activity", "activity", id, "审核通过活动：" + activity.getTitle());
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity rejectReview(Long id, RejectActivityRequest request, User user) {
        AuthUtil.requireAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!ActivityStatusUtil.PENDING_REVIEW.equals(activity.getStatus())) {
            throw new BusinessException("该活动不处于待审核状态");
        }
        activity.setStatus(ActivityStatusUtil.REJECTED);
        activity.setRejectReason(request.getReason().trim());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "reject_activity", "activity", id, "驳回活动：" + activity.getTitle() + "；原因：" + activity.getRejectReason());
        applyResponseDefaults(activity);
        return activity;
    }

    @Transactional
    public Activity cancel(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        if (ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity))) {
            throw new BusinessException("活动已结束，不能取消");
        }
        activity.setStatus(ActivityStatusUtil.CANCELLED);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "cancel_activity", "activity", id, "取消活动：" + activity.getTitle());
        noticeService.notifyActivityCancelled(activity, user);
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity qrCodeActivity(Long id, User user) {
        return requireManageableActivity(id, user);
    }

    public String generateCheckinCode(Long id, User user) {
        Activity activity = requireManageableActivity(id, user);
        activity.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(user, "refresh_checkin_qr", "activity", id, "刷新签到二维码：" + activity.getTitle());
        return activity.getCheckinCode();
    }

    public Activity requireActivity(Long id) {
        Activity activity = requireStoredActivity(id);
        applyResponseDefaults(activity);
        return activity;
    }

    private Activity requireStoredActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    public Activity requireManageableActivity(Long id, User user) {
        AuthUtil.requireOrganizerOrAdmin(user);
        Activity activity = requireStoredActivity(id);
        if (!"admin".equals(user.getRole()) && !Objects.equals(activity.getCreatorId(), user.getId())) {
            throw new BusinessException("只能管理自己创建的活动");
        }
        return activity;
    }

    public Long countRegistrations(Long activityId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId));
    }

    public Long countCheckins(Long activityId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId));
    }

    private boolean hasRegistration(Long activityId, Long userId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)) > 0;
    }

    private boolean hasCheckin(Long activityId, Long userId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, userId)) > 0;
    }

    private String normalizeActivityMode(String activityMode) {
        return MODE_ONLINE.equalsIgnoreCase(activityMode) ? MODE_ONLINE : MODE_OFFLINE;
    }

    private void ensureCheckinCodeIfPublished(Activity activity) {
        if (!ActivityStatusUtil.PUBLISHED.equals(activity.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(activity.getCheckinCode())) {
            activity.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private void applyResponseDefaults(Activity activity) {
        activity.setReviewStatus(activity.getStatus());
        ActivityStatusUtil.applyCalculatedStatus(activity);
        applyActivityModeDefault(activity);
    }

    private void applyActivityModeDefault(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityMode())) {
            activity.setActivityMode(MODE_OFFLINE);
        }
    }

    private boolean isManager(User user) {
        return user != null && ("organizer".equals(user.getRole()) || "admin".equals(user.getRole()));
    }

    private boolean canViewActivity(Activity activity, User user) {
        if (ActivityStatusUtil.PUBLISHED.equals(activity.getReviewStatus())) {
            return true;
        }
        return user != null && ("admin".equals(user.getRole())
                || ("organizer".equals(user.getRole()) && Objects.equals(activity.getCreatorId(), user.getId())));
    }
}
