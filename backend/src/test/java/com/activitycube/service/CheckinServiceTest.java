package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.dto.ManualCheckinRequest;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.UserMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinServiceTest {
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final CheckinService checkinService = new CheckinService(checkinMapper, registrationService, activityService, userMapper, operationLogService, noticeService);

    @Test
    void rejectsCheckinBeforeCheckinStartTime() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setCheckinStartTime(LocalDateTime.now().plusHours(1));
        activity.setCheckinEndTime(LocalDateTime.now().plusHours(2));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到尚未开始");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsCheckinAfterCheckinEndTime() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setActivityMode("online");
        activity.setCheckinStartTime(LocalDateTime.now().minusHours(2));
        activity.setCheckinEndTime(LocalDateTime.now().minusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到已结束");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsCheckinWhenActivityCancelled() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CANCELLED");
        activity.setCheckinStartTime(LocalDateTime.now().minusHours(1));
        activity.setCheckinEndTime(LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前活动已取消");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsCheckinWhenActivityEnded() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("PUBLISHED");
        activity.setRegisterStartTime(LocalDateTime.now().minusHours(5));
        activity.setRegisterEndTime(LocalDateTime.now().minusHours(4));
        activity.setStartTime(LocalDateTime.now().minusHours(3));
        activity.setEndTime(LocalDateTime.now().minusHours(2));
        activity.setCheckinStartTime(LocalDateTime.now().minusHours(1));
        activity.setCheckinEndTime(LocalDateTime.now().plusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前活动已结束");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsOfflineCheckinWithoutValidCode() {
        Activity activity = activeActivity("offline");
        activity.setCheckinCode("abc123");
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该活动需现场扫码签到");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsOfflineCheckinWithInvalidCode() {
        Activity activity = activeActivity("offline");
        activity.setCheckinCode("abc123");
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), "wrong-code"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到二维码无效");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void acceptsOfflineCheckinWithValidCode() {
        Activity activity = activeActivity("offline");
        activity.setCheckinCode("abc123");
        Registration registration = new Registration();
        registration.setId(8L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationService.requireRegistration(1L, 3L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        checkinService.checkin(1L, user(), "abc123");

        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getRegistrationId()).isEqualTo(8L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getCheckinType()).isEqualTo("qr");
    }

    @Test
    void acceptsOnlineCheckinWithoutCode() {
        Activity activity = activeActivity("online");
        Registration registration = new Registration();
        registration.setId(9L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationService.requireRegistration(1L, 3L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        checkinService.checkin(1L, user(), null);

        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getRegistrationId()).isEqualTo(9L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getCheckinType()).isEqualTo("online");
    }

    @Test
    void acceptsHybridActivityOnlineCheckinByDefault() {
        Activity activity = activeActivity("hybrid");
        Registration registration = new Registration();
        registration.setId(10L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationService.requireRegistration(1L, 3L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        checkinService.checkin(1L, user(), null);

        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        assertThat(captor.getValue().getCheckinType()).isEqualTo("online");
    }

    @Test
    void rejectsQrCheckinWhenActivityOnlySupportsOnlineCheckin() {
        Activity activity = activeActivity("online");
        activity.setCheckinMode("online");
        activity.setCheckinCode("abc123");
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), "abc123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该活动不支持现场扫码签到");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
    }

    @Test
    void rejectsDuplicateCheckinWithFriendlyMessage() {
        Activity activity = activeActivity("online");
        Registration registration = new Registration();
        registration.setId(9L);
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationService.requireRegistration(1L, 3L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已完成签到，请勿重复签到");
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void rejectsExpiredOfflineQrCodeAfterCheckinEndTime() {
        Activity activity = activeActivity("offline");
        activity.setCheckinCode("abc123");
        activity.setCheckinStartTime(LocalDateTime.now().minusHours(2));
        activity.setCheckinEndTime(LocalDateTime.now().minusHours(1));
        when(activityService.requireActivity(1L)).thenReturn(activity);

        assertThatThrownBy(() -> checkinService.checkin(1L, user(), "abc123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到二维码已过期，请重新扫码");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }

    @Test
    void createsManualCheckinForRegisteredStudent() {
        Activity activity = activeActivity("offline");
        activity.setCreatorId(8L);
        Registration registration = new Registration();
        registration.setId(15L);
        User operator = user();
        operator.setRole("organizer");
        operator.setRealName("活动负责人");
        User student = new User();
        student.setId(7L);
        student.setRole("student");
        student.setCampus("文化路校区");

        when(activityService.requireManageableActivity(1L, operator)).thenReturn(activity);
        when(userMapper.selectById(7L)).thenReturn(student);
        when(registrationService.requireRegistration(1L, 7L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        checkinService.manualCheckin(1L, manualRequest(7L, "手机没电，现场核验后补签"), operator);

        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinMapper).insert(captor.capture());
        Checkin created = captor.getValue();
        assertThat(created.getUserId()).isEqualTo(7L);
        assertThat(created.getRegistrationId()).isEqualTo(15L);
        assertThat(created.getCheckinType()).isEqualTo("manual");
        assertThat(created.getOperatorId()).isEqualTo(3L);
        assertThat(created.getOperatorName()).isEqualTo("活动负责人");
        assertThat(created.getRemark()).isEqualTo("手机没电，现场核验后补签");
        verify(noticeService).notifyManualCheckin(activity, student, operator);
    }

    @Test
    void rejectsManualCheckinWithoutRemark() {
        assertThatThrownBy(() -> checkinService.manualCheckin(1L, manualRequest(7L, " "), user()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("补签原因不能为空");
        verify(activityService, never()).requireManageableActivity(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsManualCheckinForStudentWithoutRegistration() {
        Activity activity = activeActivity("offline");
        User operator = user();
        operator.setRole("admin");
        User student = new User();
        student.setId(7L);
        student.setRole("student");
        when(activityService.requireManageableActivity(1L, operator)).thenReturn(activity);
        when(userMapper.selectById(7L)).thenReturn(student);
        when(registrationService.requireRegistration(1L, 7L)).thenThrow(new BusinessException("你尚未报名，不能签到"));

        assertThatThrownBy(() -> checkinService.manualCheckin(1L, manualRequest(7L, "现场确认"), operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该学生未报名，不能补签");
    }

    @Test
    void rejectsDuplicateManualCheckin() {
        Activity activity = activeActivity("offline");
        User operator = user();
        operator.setRole("admin");
        User student = new User();
        student.setId(7L);
        student.setRole("student");
        Registration registration = new Registration();
        registration.setId(15L);
        when(activityService.requireManageableActivity(1L, operator)).thenReturn(activity);
        when(userMapper.selectById(7L)).thenReturn(student);
        when(registrationService.requireRegistration(1L, 7L)).thenReturn(registration);
        when(checkinMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        assertThatThrownBy(() -> checkinService.manualCheckin(1L, manualRequest(7L, "现场确认"), operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该学生已签到，请勿重复补签");
    }

    @Test
    void hidesOwnershipDetailsWhenManualCheckinIsNotAllowed() {
        User operator = user();
        operator.setRole("organizer");
        when(activityService.requireManageableActivity(1L, operator))
                .thenThrow(new BusinessException("只能管理自己创建的活动"));

        assertThatThrownBy(() -> checkinService.manualCheckin(1L, manualRequest(7L, "现场确认"), operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你没有权限补签该活动");
    }

    private ManualCheckinRequest manualRequest(Long userId, String remark) {
        ManualCheckinRequest request = new ManualCheckinRequest();
        request.setUserId(userId);
        request.setRemark(remark);
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setCampus("龙子湖校区");
        return user;
    }

    private Activity activeActivity(String mode) {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("PUBLISHED");
        activity.setActivityMode(mode);
        activity.setRegisterStartTime(LocalDateTime.now().minusHours(4));
        activity.setRegisterEndTime(LocalDateTime.now().minusHours(3));
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        activity.setCheckinStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setCheckinEndTime(LocalDateTime.now().plusMinutes(50));
        return activity;
    }
}
