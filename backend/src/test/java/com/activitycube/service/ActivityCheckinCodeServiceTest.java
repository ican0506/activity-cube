package com.activitycube.service;

import com.activitycube.entity.Activity;
import com.activitycube.entity.User;
import com.activitycube.dto.ActivityRequest;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.dto.RejectActivityRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityCheckinCodeServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final ActivityService activityService = new ActivityService(activityMapper, registrationMapper, checkinMapper, feedbackMapper, operationLogService, noticeService);

    @Test
    void managerCanGenerateANewCheckinCodeForOwnActivity() {
        Activity activity = new Activity();
        activity.setId(8L);
        activity.setCreatorId(5L);
        activity.setStatus("PUBLISHED");
        when(activityMapper.selectById(8L)).thenReturn(activity);

        String checkinCode = activityService.generateCheckinCode(8L, manager());

        assertThat(checkinCode).isNotBlank();
        assertThat(activity.getCheckinCode()).isEqualTo(checkinCode);
        verify(activityMapper).updateById(activity);
    }

    @Test
    void organizerCanSubmitRejectedActivityForReview() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setCreatorId(5L);
        activity.setStatus("REJECTED");
        activity.setRejectReason("时间说明不足");
        when(activityMapper.selectById(9L)).thenReturn(activity);

        Activity submitted = activityService.submitReview(9L, manager());

        assertThat(submitted.getReviewStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(submitted.getRejectReason()).isNull();
        verify(operationLogService).record(manager(), "submit_activity_review", "activity", 9L, "提交活动审核：null");
    }

    @Test
    void adminCanRejectPendingActivityWithReason() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus("PENDING_REVIEW");
        when(activityMapper.selectById(10L)).thenReturn(activity);
        RejectActivityRequest request = new RejectActivityRequest();
        request.setReason("请补充活动预算说明");

        Activity rejected = activityService.rejectReview(10L, request, admin());

        assertThat(rejected.getReviewStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getRejectReason()).isEqualTo("请补充活动预算说明");
        verify(operationLogService).record(admin(), "reject_activity", "activity", 10L, "驳回活动：null；原因：请补充活动预算说明");
    }

    @Test
    void cancelActivityNotifiesRegisteredStudents() {
        Activity activity = new Activity();
        activity.setId(11L);
        activity.setTitle("校园活动");
        activity.setCreatorId(5L);
        activity.setStatus("PUBLISHED");
        activity.setEndTime(java.time.LocalDateTime.now().plusHours(2));
        when(activityMapper.selectById(11L)).thenReturn(activity);

        Activity cancelled = activityService.cancel(11L, manager());

        assertThat(cancelled.getReviewStatus()).isEqualTo("CANCELLED");
        verify(noticeService).notifyActivityCancelled(activity, manager());
    }

    @Test
    void publishedActivityKeyChangeNotifiesRegisteredStudentsAndRecordsLog() {
        Activity activity = publishedActivity(12L);
        when(activityMapper.selectById(12L)).thenReturn(activity);
        ActivityRequest request = requestFrom(activity);
        request.setLocation("文化路校区报告厅");

        Activity updated = activityService.update(12L, request, manager());

        assertThat(updated.getLocation()).isEqualTo("文化路校区报告厅");
        verify(activityMapper).updateById(activity);
        verify(operationLogService).record(manager(), "update_published_activity", "activity", 12L,
                "更新已发布活动：校园活动");
        verify(noticeService).notifyActivityUpdated(activity, manager(), List.of("活动地点"));
    }

    @Test
    void organizerCannotChangePublishedCoreFields() {
        Activity activity = publishedActivity(13L);
        activity.setActivityCategory("公益活动");
        activity.setActivityMode("offline");
        activity.setRewardEnabled(true);
        activity.setRewardType("课外学时");
        activity.setRewardHours(BigDecimal.valueOf(2));
        when(activityMapper.selectById(13L)).thenReturn(activity);
        ActivityRequest request = requestFrom(activity);
        request.setTitle("负责人不应修改的标题");
        LocalDateTime originalRegisterStart = activity.getRegisterStartTime();
        request.setRegisterStartTime(originalRegisterStart.minusDays(1));
        request.setActivityCategory("竞赛活动");
        request.setActivityMode("online");
        request.setRewardType("积分");
        request.setRewardHours(BigDecimal.valueOf(0));
        request.setRewardPoints(20);

        Activity updated = activityService.update(13L, request, manager());

        assertThat(updated.getTitle()).isEqualTo("校园活动");
        assertThat(updated.getRegisterStartTime()).isEqualTo(originalRegisterStart);
        assertThat(updated.getActivityCategory()).isEqualTo("公益活动");
        assertThat(updated.getActivityMode()).isEqualTo("offline");
        assertThat(updated.getRewardType()).isEqualTo("课外学时");
        assertThat(updated.getRewardHours()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    void pendingReviewActivityCanBeEditedWithoutChangingReviewStatus() {
        Activity activity = publishedActivity(14L);
        activity.setStatus("PENDING_REVIEW");
        when(activityMapper.selectById(14L)).thenReturn(activity);
        ActivityRequest request = requestFrom(activity);
        request.setTitle("修改后的活动名称");

        Activity updated = activityService.update(14L, request, manager());

        assertThat(updated.getTitle()).isEqualTo("修改后的活动名称");
        assertThat(updated.getReviewStatus()).isEqualTo("PENDING_REVIEW");
        verify(activityMapper).updateById(activity);
    }

    private User manager() {
        User user = new User();
        user.setId(5L);
        user.setRole("organizer");
        return user;
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setRole("admin");
        return user;
    }

    private Activity publishedActivity(Long id) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle("校园活动");
        activity.setDescription("活动说明");
        activity.setCreatorId(5L);
        activity.setStatus("PUBLISHED");
        activity.setActivityCategory("公益活动");
        activity.setActivityMode("offline");
        activity.setCampus("龙子湖校区");
        activity.setLocation("龙子湖校区报告厅");
        activity.setRegisterStartTime(LocalDateTime.now().minusDays(1));
        activity.setRegisterEndTime(LocalDateTime.now().plusDays(1));
        activity.setStartTime(LocalDateTime.now().plusDays(2));
        activity.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        activity.setCheckinStartTime(LocalDateTime.now().plusDays(2).minusMinutes(15));
        activity.setCheckinEndTime(LocalDateTime.now().plusDays(2).plusHours(2).plusMinutes(15));
        activity.setMaxParticipants(100);
        activity.setAllowCrossCampus(true);
        activity.setRewardEnabled(false);
        activity.setRewardType("无");
        activity.setRewardHours(BigDecimal.ZERO);
        activity.setRewardPoints(0);
        return activity;
    }

    private ActivityRequest requestFrom(Activity activity) {
        ActivityRequest request = new ActivityRequest();
        request.setTitle(activity.getTitle());
        request.setDescription(activity.getDescription());
        request.setCoverUrl(activity.getCoverUrl());
        request.setActivityMode(activity.getActivityMode());
        request.setActivityCategory(activity.getActivityCategory());
        request.setCampus(activity.getCampus());
        request.setLocation(activity.getLocation());
        request.setRegisterStartTime(activity.getRegisterStartTime());
        request.setRegisterEndTime(activity.getRegisterEndTime());
        request.setStartTime(activity.getStartTime());
        request.setEndTime(activity.getEndTime());
        request.setCheckinStartTime(activity.getCheckinStartTime());
        request.setCheckinEndTime(activity.getCheckinEndTime());
        request.setMaxParticipants(activity.getMaxParticipants());
        request.setAllowCrossCampus(activity.getAllowCrossCampus());
        request.setRewardEnabled(activity.getRewardEnabled());
        request.setRewardType(activity.getRewardType());
        request.setRewardHours(activity.getRewardHours());
        request.setRewardPoints(activity.getRewardPoints());
        request.setRewardDescription(activity.getRewardDescription());
        request.setStatus(activity.getStatus());
        return request;
    }
}
