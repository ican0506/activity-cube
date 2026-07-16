package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.RegisterRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.RegistrationMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final RegistrationService registrationService = new RegistrationService(registrationMapper, activityService);

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
