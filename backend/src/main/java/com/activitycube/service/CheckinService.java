package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckinService {
    private final CheckinMapper checkinMapper;
    private final RegistrationService registrationService;
    private final ActivityService activityService;

    public Checkin checkin(Long activityId, User user, String checkinCode) {
        Activity activity = activityService.requireActivity(activityId);
        validateActivityStatus(activity);
        validateCheckinTime(activity);
        validateOfflineCheckinCode(activity, checkinCode);
        Registration registration = registrationService.requireRegistration(activityId, user.getId());
        if (checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, user.getId())) > 0) {
            throw new BusinessException("你已签到，请勿重复提交");
        }
        Checkin checkin = new Checkin();
        checkin.setActivityId(activityId);
        checkin.setUserId(user.getId());
        checkin.setRegistrationId(registration.getId());
        checkin.setCampus(user.getCampus());
        checkin.setCheckinTime(LocalDateTime.now());
        checkinMapper.insert(checkin);
        return checkin;
    }

    private void validateActivityStatus(Activity activity) {
        String status = ActivityStatusUtil.calculateStatus(activity);
        if (ActivityStatusUtil.CANCELLED.equals(status)) {
            throw new BusinessException("当前活动已取消");
        }
        if (ActivityStatusUtil.ENDED.equals(status)) {
            throw new BusinessException("当前活动已结束");
        }
        if (ActivityStatusUtil.DRAFT.equals(status)) {
            throw new BusinessException("活动尚未发布");
        }
    }

    private void validateCheckinTime(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkinStartTime = activity.getCheckinStartTime() == null ? activity.getStartTime() : activity.getCheckinStartTime();
        LocalDateTime checkinEndTime = activity.getCheckinEndTime() == null ? activity.getEndTime() : activity.getCheckinEndTime();
        if (checkinStartTime != null && now.isBefore(checkinStartTime)) {
            throw new BusinessException("签到尚未开始");
        }
        if (checkinEndTime != null && now.isAfter(checkinEndTime)) {
            if (isOffline(activity)) {
                throw new BusinessException("签到二维码已过期，请重新扫码");
            }
            throw new BusinessException("签到已结束");
        }
    }

    private void validateOfflineCheckinCode(Activity activity, String checkinCode) {
        if (!isOffline(activity)) {
            return;
        }
        if (!StringUtils.hasText(checkinCode) || !checkinCode.equals(activity.getCheckinCode())) {
            throw new BusinessException("线下活动需扫描现场签到二维码");
        }
    }

    private boolean isOffline(Activity activity) {
        return !ActivityService.MODE_ONLINE.equals(activity.getActivityMode());
    }

    public List<Checkin> listByActivity(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        return checkins(activityId);
    }

    public List<Registration> absences(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        Set<Long> checkedUserIds = checkins(activityId).stream()
                .map(Checkin::getUserId)
                .collect(Collectors.toSet());
        return registrationService.registrations(activityId).stream()
                .filter(registration -> !checkedUserIds.contains(registration.getUserId()))
                .toList();
    }

    public List<Checkin> checkins(Long activityId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .orderByDesc(Checkin::getCheckinTime));
    }

    public List<Checkin> myCheckins(User user) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, user.getId())
                .orderByDesc(Checkin::getCheckinTime));
    }
}
