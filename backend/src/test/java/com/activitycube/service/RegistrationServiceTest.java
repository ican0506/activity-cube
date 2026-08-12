package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.RegisterRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.CheckinMapper;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final RegistrationService registrationService = new RegistrationService(registrationMapper, activityService, checkinMapper, operationLogService, noticeService, activityMapper);

    @Test
    void rejectsRegistrationBeforeRegistrationStartTime() {
        Activity activity = activity(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前活动未开始报名");

        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void rejectsRegistrationAfterRegistrationEndTime() {
        Activity activity = activity(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前活动报名已结束");

        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void rejectsDuplicateStudentNoInSameActivity() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectCount(any())).thenReturn(0L, 1L);

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已报名，请勿重复提交");

        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void sendsNoticeAfterSuccessfulRegistration() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        activity.setTitle("校园讲座");
        User student = user();
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectCount(any())).thenReturn(0L);
        when(activityMapper.tryReserveRegistrationSlot(1L)).thenReturn(1);

        Registration registration = registrationService.register(1L, request(), student);

        verify(activityMapper).tryReserveRegistrationSlot(1L);
        verify(registrationMapper).insert(registration);
        verify(noticeService).notifyRegistrationSuccess(activity, registration, student);
    }

    @Test
    void rejectsRegistrationWhenAtomicSlotReserveFailsAndNoDuplicateExists() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);
        when(activityMapper.tryReserveRegistrationSlot(1L)).thenReturn(0);

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该活动人数已满");

        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void reportsDuplicateWhenSlotReserveFailsButCurrentUserAlreadyRegistered() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectCount(any())).thenReturn(0L, 0L, 1L);
        when(activityMapper.tryReserveRegistrationSlot(1L)).thenReturn(0);

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已报名，请勿重复提交");

        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void convertsDuplicateKeyExceptionToFriendlyRegistrationMessage() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectCount(any())).thenReturn(0L);
        when(activityMapper.tryReserveRegistrationSlot(1L)).thenReturn(1);
        when(registrationMapper.insert(any(Registration.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> registrationService.register(1L, request(), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已报名，请勿重复提交");

        verify(noticeService, never()).notifyRegistrationSuccess(any(), any(), any());
    }

    @Test
    void cancelsOwnRegistrationBeforeDeadlineWhenNotCheckedIn() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        Registration registration = new Registration();
        registration.setId(12L);
        registration.setActivityId(1L);
        registration.setUserId(3L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(checkinMapper.selectCount(any())).thenReturn(0L);
        when(activityMapper.releaseRegistrationSlot(1L)).thenReturn(1);
        when(registrationMapper.deleteById(12L)).thenReturn(1);

        registrationService.cancelMyRegistration(1L, user());

        verify(activityMapper).releaseRegistrationSlot(1L);
        verify(registrationMapper).deleteById(12L);
        verify(operationLogService).record(user(), "cancel_registration", "registration", 12L, "取消报名：null");
    }

    @Test
    void allowsCancellationWhenCounterIsAlreadyZeroButRegistrationExists() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        Registration registration = new Registration();
        registration.setId(12L);
        registration.setActivityId(1L);
        registration.setUserId(3L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(checkinMapper.selectCount(any())).thenReturn(0L);
        when(activityMapper.releaseRegistrationSlot(1L)).thenReturn(0);
        when(registrationMapper.deleteById(12L)).thenReturn(1);

        registrationService.cancelMyRegistration(1L, user());

        verify(activityMapper).releaseRegistrationSlot(1L);
        verify(registrationMapper).deleteById(12L);
    }

    @Test
    void rollsBackCancellationWhenDeleteAffectsNoRows() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        Registration registration = new Registration();
        registration.setId(12L);
        registration.setActivityId(1L);
        registration.setUserId(3L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(checkinMapper.selectCount(any())).thenReturn(0L);
        when(activityMapper.releaseRegistrationSlot(1L)).thenReturn(1);
        when(registrationMapper.deleteById(12L)).thenReturn(0);

        assertThatThrownBy(() -> registrationService.cancelMyRegistration(1L, user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未找到你的报名记录");

        verify(operationLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsCancellationAfterStudentHasCheckedIn() {
        Activity activity = activity(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        Registration registration = new Registration();
        registration.setId(12L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(checkinMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> registrationService.cancelMyRegistration(1L, user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已完成签到，不能取消报名");
        verify(registrationMapper, never()).deleteById(12L);
    }

    private Activity activity(LocalDateTime registerStartTime, LocalDateTime registerEndTime) {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("PUBLISHED");
        activity.setCampus("全校区");
        activity.setAllowCrossCampus(true);
        activity.setRegisterStartTime(registerStartTime);
        activity.setRegisterEndTime(registerEndTime);
        activity.setStartTime(LocalDateTime.now().plusHours(3));
        activity.setEndTime(LocalDateTime.now().plusHours(5));
        return activity;
    }

    private RegisterRequest request() {
        RegisterRequest request = new RegisterRequest();
        request.setName("张三");
        request.setStudentNo("2026001");
        request.setCollege("信息工程学院");
        request.setMajorClass("软件 2301");
        request.setPhone("13800000001");
        request.setCampus("龙子湖校区");
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setCampus("龙子湖校区");
        return user;
    }
}
