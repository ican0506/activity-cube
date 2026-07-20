package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.Activity;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.vo.CheckinCodeResolveResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScanServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final ScanService scanService = new ScanService(activityMapper);

    @Test
    void resolvesAnActiveCheckinCodeToItsActivity() {
        Activity activity = activeActivity();
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        CheckinCodeResolveResult result = scanService.resolveCheckinCode("abc123");

        assertThat(result.getActivityId()).isEqualTo(8L);
        assertThat(result.getActivityName()).isEqualTo("校园活动");
        assertThat(result.getActivityMode()).isEqualTo("offline");
        assertThat(result.getCheckinCode()).isEqualTo("abc123");
    }

    @Test
    void rejectsAnUnknownCheckinCode() {
        when(activityMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> scanService.resolveCheckinCode("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到码无效");
    }

    @Test
    void rejectsAResolvedCodeWhenItsActivityIsCancelled() {
        Activity activity = activeActivity();
        activity.setStatus("CANCELLED");
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        assertThatThrownBy(() -> scanService.resolveCheckinCode("abc123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动已取消，无法签到");
    }

    @Test
    void rejectsAResolvedCodeAfterItsCheckinWindowEnds() {
        Activity activity = activeActivity();
        activity.setCheckinEndTime(LocalDateTime.now().minusMinutes(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        assertThatThrownBy(() -> scanService.resolveCheckinCode("abc123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到码已过期，请重新获取");
    }

    private Activity activeActivity() {
        Activity activity = new Activity();
        activity.setId(8L);
        activity.setTitle("校园活动");
        activity.setStatus("PUBLISHED");
        activity.setActivityMode("offline");
        activity.setCheckinCode("abc123");
        activity.setRegisterStartTime(LocalDateTime.now().minusHours(4));
        activity.setRegisterEndTime(LocalDateTime.now().minusHours(3));
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        activity.setCheckinStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setCheckinEndTime(LocalDateTime.now().plusMinutes(10));
        return activity;
    }
}
