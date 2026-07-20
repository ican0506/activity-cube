package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.ManualCheckinRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

    public Checkin checkin(Long activityId, User user, String checkinCode) {
        Activity activity = activityService.requireActivity(activityId);
        validateActivityStatus(activity);
        validateCheckinTime(activity);
        validateOfflineCheckinCode(activity, checkinCode);
        Registration registration = registrationService.requireRegistration(activityId, user.getId());
        if (checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, user.getId())) > 0) {
            throw new BusinessException("你已完成签到，请勿重复签到");
        }
        Checkin checkin = new Checkin();
        checkin.setActivityId(activityId);
        checkin.setUserId(user.getId());
        checkin.setRegistrationId(registration.getId());
        checkin.setCampus(user.getCampus());
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setCheckinType(isOffline(activity) ? "qr" : "online");
        checkinMapper.insert(checkin);
        return checkin;
    }

    @Transactional
    public Checkin manualCheckin(Long activityId, ManualCheckinRequest request, User operator) {
        if (request == null || !StringUtils.hasText(request.getRemark())) {
            throw new BusinessException("补签原因不能为空");
        }

        Activity activity = requireManualCheckinPermission(activityId, operator);
        validateManualCheckinActivity(activity);
        User student = resolveStudent(request);
        Registration registration;
        try {
            registration = registrationService.requireRegistration(activityId, student.getId());
        } catch (BusinessException exception) {
            throw new BusinessException("该学生未报名，不能补签");
        }
        if (checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId)
                .eq(Checkin::getUserId, student.getId())) > 0) {
            throw new BusinessException("该学生已签到，请勿重复补签");
        }

        Checkin checkin = new Checkin();
        checkin.setActivityId(activityId);
        checkin.setUserId(student.getId());
        checkin.setRegistrationId(registration.getId());
        checkin.setCampus(student.getCampus());
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setCheckinType("manual");
        checkin.setOperatorId(operator.getId());
        checkin.setOperatorName(StringUtils.hasText(operator.getRealName())
                ? operator.getRealName()
                : operator.getUsername());
        checkin.setRemark(request.getRemark().trim());
        checkinMapper.insert(checkin);
        operationLogService.record(operator, "manual_checkin", "checkin", checkin.getId(),
                "为学生补签：" + student.getRealName() + "；活动ID：" + activityId + "；原因：" + checkin.getRemark());
        noticeService.notifyManualCheckin(activity, student, operator);
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
        if (!StringUtils.hasText(checkinCode)) {
            throw new BusinessException("线下活动需扫描现场签到二维码");
        }
        if (!checkinCode.equals(activity.getCheckinCode())) {
            throw new BusinessException("签到二维码无效");
        }
    }

    private boolean isOffline(Activity activity) {
        return !ActivityService.MODE_ONLINE.equals(activity.getActivityMode());
    }

    private Activity requireManualCheckinPermission(Long activityId, User operator) {
        try {
            return activityService.requireManageableActivity(activityId, operator);
        } catch (BusinessException exception) {
            if ("无活动负责人权限".equals(exception.getMessage())
                    || "只能管理自己创建的活动".equals(exception.getMessage())) {
                throw new BusinessException("你没有权限补签该活动");
            }
            throw exception;
        }
    }

    private void validateManualCheckinActivity(Activity activity) {
        String status = ActivityStatusUtil.calculateStatus(activity);
        if (ActivityStatusUtil.DRAFT.equals(status)) {
            throw new BusinessException("草稿活动不能补签");
        }
        if (ActivityStatusUtil.CANCELLED.equals(status)) {
            throw new BusinessException("当前活动已取消，不能补签");
        }
    }

    private User resolveStudent(ManualCheckinRequest request) {
        User student = request.getUserId() != null
                ? userMapper.selectById(request.getUserId())
                : userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getStudentNo, request.getStudentNo()));
        if (student == null || (!"student".equals(student.getRole()) && !"user".equals(student.getRole()))) {
            throw new BusinessException("学生不存在或不是学生账号");
        }
        return student;
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
