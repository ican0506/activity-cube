package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.CheckinMapper;
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
    void rejectsCheckinBeforeActivityStartTime() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        when(activityService.requireActivity(1L)).thenReturn(activity);
        when(registrationService.requireRegistration(anyLong(), anyLong())).thenReturn(new Registration());

        User user = new User();
        user.setId(3L);
        user.setCampus("龙子湖校区");

        assertThatThrownBy(() -> checkinService.checkin(1L, user))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前不在签到时间内");
        verify(registrationService, never()).requireRegistration(anyLong(), anyLong());
        verify(checkinMapper, never()).insert(org.mockito.ArgumentMatchers.any(Checkin.class));
    }
}
