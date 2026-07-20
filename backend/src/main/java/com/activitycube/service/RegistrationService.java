package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.RegisterRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationMapper registrationMapper;
    private final ActivityService activityService;
    private final CheckinMapper checkinMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

    @Transactional
    public Registration register(Long activityId, RegisterRequest request, User user) {
        Activity activity = activityService.requireActivity(activityId);
        validateRegistration(activity, request, user);
        Registration registration = new Registration();
        BeanUtils.copyProperties(request, registration);
        registration.setActivityId(activityId);
        registration.setUserId(user.getId());
        registrationMapper.insert(registration);
        noticeService.notifyRegistrationSuccess(activity, registration, user);
        return registration;
    }

    public List<Registration> listByActivity(Long activityId, User user) {
        activityService.requireManageableActivity(activityId, user);
        return registrations(activityId);
    }

    public List<Registration> myRegistrations(User user) {
        return registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, user.getId())
                .orderByDesc(Registration::getCreatedAt)).stream()
                .peek(registration -> applyCancellationInfo(registration, user))
                .toList();
    }

    public void cancelMyRegistration(Long activityId, User user) {
        Activity activity = activityService.requireActivity(activityId);
        Registration registration = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, user.getId()));
        if (registration == null) {
            throw new BusinessException("未找到你的报名记录");
        }
        String reason = cancellationReason(activity, user.getId());
        if (reason != null) {
            throw new BusinessException(reason);
        }
        registrationMapper.deleteById(registration.getId());
        operationLogService.record(user, "cancel_registration", "registration", registration.getId(), "取消报名：" + activity.getTitle());
    }

    public List<Registration> registrations(Long activityId) {
        return registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .orderByDesc(Registration::getCreatedAt));
    }

    public Registration requireRegistration(Long activityId, Long userId) {
        Registration registration = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId));
        if (registration == null) {
            throw new BusinessException("你尚未报名，不能签到");
        }
        return registration;
    }

    private void validateRegistration(Activity activity, RegisterRequest request, User user) {
        String calculatedStatus = ActivityStatusUtil.calculateStatus(activity);
        if (!ActivityStatusUtil.REGISTERING.equals(calculatedStatus)) {
            throw new BusinessException(registrationClosedMessage(calculatedStatus));
        }
        if (registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activity.getId())
                .eq(Registration::getUserId, user.getId())) > 0) {
            throw new BusinessException("你已报名，请勿重复提交");
        }
        if (registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activity.getId())
                .eq(Registration::getStudentNo, request.getStudentNo())) > 0) {
            throw new BusinessException("你已报名，请勿重复提交");
        }
        if (activity.getMaxParticipants() != null
                && activityService.countRegistrations(activity.getId()) >= activity.getMaxParticipants()) {
            throw new BusinessException("该活动人数已满");
        }
        boolean campusLimited = !Boolean.TRUE.equals(activity.getAllowCrossCampus())
                && !"全校区".equals(activity.getCampus())
                && !"线上".equals(activity.getCampus());
        if (campusLimited && !activity.getCampus().equals(user.getCampus())) {
            throw new BusinessException("该活动不允许跨校区报名");
        }
    }

    private String registrationClosedMessage(String status) {
        return switch (status) {
            case ActivityStatusUtil.DRAFT -> "当前活动尚未发布";
            case ActivityStatusUtil.CANCELLED -> "当前活动已取消";
            case ActivityStatusUtil.NOT_STARTED -> "当前活动未开始报名";
            case ActivityStatusUtil.WAITING_START, ActivityStatusUtil.ONGOING -> "当前活动报名已结束";
            case ActivityStatusUtil.ENDED -> "当前活动已结束";
            default -> "当前活动不能报名";
        };
    }

    private void applyCancellationInfo(Registration registration, User user) {
        try {
            Activity activity = activityService.requireActivity(registration.getActivityId());
            registration.setActivityTitle(activity.getTitle());
            registration.setActivityStatus(activity.getStatus());
            String reason = cancellationReason(activity, user.getId());
            registration.setCanCancel(reason == null);
            registration.setCancelReason(reason);
        } catch (BusinessException exception) {
            registration.setCanCancel(false);
            registration.setCancelReason("活动不存在或不可取消");
        }
    }

    private String cancellationReason(Activity activity, Long userId) {
        if (checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activity.getId())
                .eq(Checkin::getUserId, userId)) > 0) {
            return "已完成签到，不能取消报名";
        }
        String status = ActivityStatusUtil.calculateStatus(activity);
        if (ActivityStatusUtil.CANCELLED.equals(status)) {
            return "当前活动已取消，不能取消报名";
        }
        if (ActivityStatusUtil.ENDED.equals(status)) {
            return "当前活动已结束，不能取消报名";
        }
        if (ActivityStatusUtil.DRAFT.equals(status) || ActivityStatusUtil.PENDING_REVIEW.equals(status)
                || ActivityStatusUtil.REJECTED.equals(status)) {
            return "当前活动尚未发布，不能取消报名";
        }
        LocalDateTime deadline = activity.getRegisterEndTime();
        if (deadline != null && !LocalDateTime.now().isBefore(deadline)) {
            return "报名截止后不可取消报名";
        }
        if (ActivityStatusUtil.ONGOING.equals(status) || ActivityStatusUtil.WAITING_START.equals(status)) {
            return "当前活动已开始或报名已截止，不能取消报名";
        }
        return null;
    }
}
