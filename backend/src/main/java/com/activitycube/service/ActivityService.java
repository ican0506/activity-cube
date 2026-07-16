package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ActivityRequest;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {
    public static final String MODE_ONLINE = "online";
    public static final String MODE_OFFLINE = "offline";

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;

    public List<Activity> list(String keyword, String campus, String status) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .orderByDesc(Activity::getCreatedAt);
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
        activity.setStatus(ActivityStatusUtil.normalizeManualStatus(request.getStatus()));
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        ensureCheckinCodeIfPublished(activity);
        activity.setCreatorId(creator.getId());
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity update(Long id, ActivityRequest request, User user) {
        Activity activity = requireManageableActivity(id, user);
        BeanUtils.copyProperties(request, activity);
        activity.setStatus(ActivityStatusUtil.normalizeManualStatus(request.getStatus()));
        activity.setActivityMode(normalizeActivityMode(request.getActivityMode()));
        ensureCheckinCodeIfPublished(activity);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        applyResponseDefaults(activity);
        return activity;
    }

    public void delete(Long id, User user) {
        requireManageableActivity(id, user);
        activityMapper.deleteById(id);
    }

    public Activity requireActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        applyResponseDefaults(activity);
        return activity;
    }

    public Activity requireManageableActivity(Long id, User user) {
        AuthUtil.requireOrganizerOrAdmin(user);
        Activity activity = requireActivity(id);
        if (!"admin".equals(user.getRole()) && !activity.getCreatorId().equals(user.getId())) {
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
        if (ActivityStatusUtil.DRAFT.equals(activity.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(activity.getCheckinCode())) {
            activity.setCheckinCode(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private void applyResponseDefaults(Activity activity) {
        ActivityStatusUtil.applyCalculatedStatus(activity);
        applyActivityModeDefault(activity);
    }

    private void applyActivityModeDefault(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityMode())) {
            activity.setActivityMode(MODE_OFFLINE);
        }
    }
}
