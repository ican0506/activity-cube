package com.activitycube.service;

import com.activitycube.entity.Activity;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.dto.RejectActivityRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityCheckinCodeServiceTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final NoticeService noticeService = mock(NoticeService.class);
    private final ActivityService activityService = new ActivityService(activityMapper, registrationMapper, checkinMapper, operationLogService, noticeService);

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
}
