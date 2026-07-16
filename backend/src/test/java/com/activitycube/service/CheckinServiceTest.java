package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinServiceTest {
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final CheckinService checkinService = new CheckinService(checkinMapper, registrationService, activityService);

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
                .hasMessage("线下活动需扫描现场签到二维码");
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
