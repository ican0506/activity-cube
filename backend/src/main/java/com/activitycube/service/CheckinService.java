package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Checkin checkin(Long activityId, User user) {
        activityService.requireActivity(activityId);
        Registration registration = registrationService.requireRegistration(activityId, user.getId());
        if (checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, user.getId())) > 0) {
            throw new BusinessException("不能重复签到");
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
