package com.activitycube.service;

import com.activitycube.entity.Activity;
import com.activitycube.entity.User;
import com.activitycube.dto.ActivityQueryRequest;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.util.UserContext;
import com.activitycube.vo.ActivityCountVO;
import com.activitycube.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ActivityServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final ActivityService activityService = new ActivityService(activityMapper, registrationMapper, checkinMapper, feedbackMapper, operationLogService, noticeService);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void listReturnsEmptyWithoutCallingBatchMappersWhenNoActivities() {
        UserContext.set(student());
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Activity> activities = activityService.list(null, null, null);

        assertThat(activities).isEmpty();
        verify(registrationMapper, never()).countByActivityIds(any());
        verify(checkinMapper, never()).countByActivityIds(any());
        verify(registrationMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(checkinMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(feedbackMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
    }

    @Test
    void listAppliesStudentStateFromBatchQueryResults() {
        UserContext.set(student());
        Activity a = publishedActivity(1L, -2, 2, -1, 1, 10);
        Activity b = publishedActivity(2L, -2, 2, -1, 1, 10);
        Activity c = publishedActivity(3L, -2, 2, -1, 1, 10);
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b, c));
        when(registrationMapper.countByActivityIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(count(1L, 3L), count(3L, 5L)));
        when(checkinMapper.countByActivityIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(count(1L, 2L), count(3L, 4L)));
        when(registrationMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(1L, 3L));
        when(checkinMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(1L));
        when(feedbackMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(1L));

        List<Activity> activities = activityService.list(null, null, null);

        assertThat(activities).hasSize(3);
        assertActivity(activities.get(0), 3L, 2L, true, true, true, false, false, false, false, "已签到");
        assertActivity(activities.get(1), 0L, 0L, false, false, false, true, false, false, false, "立即报名");
        assertActivity(activities.get(2), 5L, 4L, true, false, false, false, true, true, false, "去签到");
        verify(registrationMapper).countByActivityIds(List.of(1L, 2L, 3L));
        verify(checkinMapper).countByActivityIds(List.of(1L, 2L, 3L));
        verify(registrationMapper).findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L));
        verify(checkinMapper).findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L));
        verify(feedbackMapper).findActivityIdsByUserAndActivityIds(7L, List.of(1L, 2L, 3L));
    }

    @Test
    void listForOrganizerDoesNotQueryStudentState() {
        UserContext.set(user(5L, "organizer"));
        Activity activity = publishedActivity(1L, -2, 2, -1, 1, 10);
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activity));
        when(registrationMapper.countByActivityIds(List.of(1L))).thenReturn(List.of(count(1L, 8L)));
        when(checkinMapper.countByActivityIds(List.of(1L))).thenReturn(List.of(count(1L, 6L)));

        List<Activity> activities = activityService.list(null, null, null);

        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getRegistrationCount()).isEqualTo(8L);
        assertThat(activities.get(0).getCheckinCount()).isEqualTo(6L);
        assertThat(activities.get(0).getRegistered()).isFalse();
        verify(registrationMapper).countByActivityIds(List.of(1L));
        verify(checkinMapper).countByActivityIds(List.of(1L));
        verify(registrationMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(checkinMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(feedbackMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
    }

    @Test
    void listForAdminDoesNotQueryStudentState() {
        UserContext.set(user(1L, "admin"));
        Activity activity = publishedActivity(1L, -2, 2, -1, 1, 10);
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activity));
        when(registrationMapper.countByActivityIds(List.of(1L))).thenReturn(List.of());
        when(checkinMapper.countByActivityIds(List.of(1L))).thenReturn(List.of());

        activityService.list(null, null, null);

        verify(registrationMapper).countByActivityIds(List.of(1L));
        verify(checkinMapper).countByActivityIds(List.of(1L));
        verify(registrationMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(checkinMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(feedbackMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
    }

    @Test
    void listFiltersByCalculatedStatusBeforeBatchQueries() {
        UserContext.set(student());
        Activity registering = publishedActivity(1L, -1, 1, 2, 4, 10);
        Activity waitingStart = publishedActivity(2L, -4, -3, 2, 4, 10);
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(registering, waitingStart));
        when(registrationMapper.countByActivityIds(List.of(1L))).thenReturn(List.of(count(1L, 1L)));
        when(checkinMapper.countByActivityIds(List.of(1L))).thenReturn(List.of());
        when(registrationMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());
        when(checkinMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());
        when(feedbackMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());

        List<Activity> activities = activityService.list(null, null, ActivityStatusUtil.REGISTERING);

        assertThat(activities).extracting(Activity::getId).containsExactly(1L);
        verify(registrationMapper).countByActivityIds(List.of(1L));
        verify(checkinMapper).countByActivityIds(List.of(1L));
        verify(registrationMapper).findActivityIdsByUserAndActivityIds(7L, List.of(1L));
    }

    @Test
    void listKeepsFullActivityStatusText() {
        UserContext.set(student());
        Activity full = publishedActivity(1L, -1, 1, 2, 4, 10);
        when(activityMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(full));
        when(registrationMapper.countByActivityIds(List.of(1L))).thenReturn(List.of(count(1L, 10L)));
        when(checkinMapper.countByActivityIds(List.of(1L))).thenReturn(List.of());
        when(registrationMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());
        when(checkinMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());
        when(feedbackMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());

        Activity activity = activityService.list(null, null, null).get(0);

        assertThat(activity.getCanRegister()).isFalse();
        assertThat(activity.getStudentActivityStatusText()).isEqualTo("名额已满");
    }

    @Test
    void pageNormalizesInvalidPageParamsAndEnrichesCurrentPageOnly() {
        UserContext.set(student());
        ActivityQueryRequest request = new ActivityQueryRequest();
        request.setPageNum(0);
        request.setPageSize(99);
        request.setStatus(ActivityStatusUtil.REGISTERING);
        Activity first = publishedActivity(1L, -1, 1, 2, 4, 10);
        Page<Activity> mapperPage = new Page<>(1, 50);
        mapperPage.setRecords(List.of(first));
        mapperPage.setTotal(61);
        when(activityMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperPage);
        when(registrationMapper.countByActivityIds(List.of(1L))).thenReturn(List.of(count(1L, 2L)));
        when(checkinMapper.countByActivityIds(List.of(1L))).thenReturn(List.of());
        when(registrationMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of(1L));
        when(checkinMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());
        when(feedbackMapper.findActivityIdsByUserAndActivityIds(7L, List.of(1L))).thenReturn(List.of());

        PageResult<Activity> result = activityService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(50);
        assertThat(result.getTotal()).isEqualTo(61);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRegistrationCount()).isEqualTo(2L);
        assertThat(result.getRecords().get(0).getRegistered()).isTrue();
        verify(registrationMapper).countByActivityIds(List.of(1L));
        verify(checkinMapper).countByActivityIds(List.of(1L));
        verify(registrationMapper).findActivityIdsByUserAndActivityIds(7L, List.of(1L));
    }

    @Test
    void pageReturnsEmptyWithoutCallingBatchMappersWhenCurrentPageEmpty() {
        UserContext.set(student());
        Page<Activity> mapperPage = new Page<>(2, 10);
        mapperPage.setRecords(List.of());
        mapperPage.setTotal(0);
        when(activityMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperPage);

        PageResult<Activity> result = activityService.page(new ActivityQueryRequest());

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(registrationMapper, never()).countByActivityIds(any());
        verify(checkinMapper, never()).countByActivityIds(any());
        verify(registrationMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(checkinMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
        verify(feedbackMapper, never()).findActivityIdsByUserAndActivityIds(any(), any());
    }

    private void assertActivity(Activity activity, Long registrations, Long checkins, boolean registered,
                                boolean checkedIn, boolean feedbackSubmitted, boolean canRegister,
                                boolean canCheckin, boolean canOnlineCheckin, boolean canQrCheckin,
                                String statusText) {
        assertThat(activity.getRegistrationCount()).isEqualTo(registrations);
        assertThat(activity.getCheckinCount()).isEqualTo(checkins);
        assertThat(activity.getRegistered()).isEqualTo(registered);
        assertThat(activity.getCheckedIn()).isEqualTo(checkedIn);
        assertThat(activity.getFeedbackSubmitted()).isEqualTo(feedbackSubmitted);
        assertThat(activity.getCanRegister()).isEqualTo(canRegister);
        assertThat(activity.getCanCheckin()).isEqualTo(canCheckin);
        assertThat(activity.getCanOnlineCheckin()).isEqualTo(canOnlineCheckin);
        assertThat(activity.getCanQrCheckin()).isEqualTo(canQrCheckin);
        assertThat(activity.getStudentActivityStatusText()).isEqualTo(statusText);
    }

    private ActivityCountVO count(Long activityId, Long count) {
        ActivityCountVO vo = new ActivityCountVO();
        vo.setActivityId(activityId);
        vo.setCount(count);
        return vo;
    }

    private Activity publishedActivity(Long id, int registerStartOffsetHours, int registerEndOffsetHours,
                                       int startOffsetHours, int endOffsetHours, int maxParticipants) {
        LocalDateTime now = LocalDateTime.now();
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle("活动" + id);
        activity.setDescription("活动说明");
        activity.setStatus(ActivityStatusUtil.PUBLISHED);
        activity.setActivityMode(ActivityService.MODE_ONLINE);
        activity.setCheckinMode(ActivityService.CHECKIN_MODE_ONLINE);
        activity.setActivityCategory("讲座培训");
        activity.setCampus("龙子湖校区");
        activity.setLocation("线上活动");
        activity.setRegisterStartTime(now.plusHours(registerStartOffsetHours));
        activity.setRegisterEndTime(now.plusHours(registerEndOffsetHours));
        activity.setStartTime(now.plusHours(startOffsetHours));
        activity.setEndTime(now.plusHours(endOffsetHours));
        activity.setCheckinStartTime(now.minusMinutes(10));
        activity.setCheckinEndTime(now.plusMinutes(30));
        activity.setMaxParticipants(maxParticipants);
        activity.setAllowCrossCampus(true);
        return activity;
    }

    private User student() {
        return user(7L, "student");
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
