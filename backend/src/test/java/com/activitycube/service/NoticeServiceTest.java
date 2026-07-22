package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.NoticeRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.Notice;
import com.activitycube.entity.NoticeReceiver;
import com.activitycube.entity.Registration;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.NoticeMapper;
import com.activitycube.mapper.NoticeReceiverMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeServiceTest {
    private final NoticeMapper noticeMapper = mock(NoticeMapper.class);
    private final NoticeReceiverMapper noticeReceiverMapper = mock(NoticeReceiverMapper.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final NoticeService noticeService = new NoticeService(
            noticeMapper, noticeReceiverMapper, activityMapper, registrationMapper, checkinMapper, feedbackMapper, userMapper);

    @Test
    void organizerCanPublishActivityNoticeToRegisteredStudentsOfOwnActivity() {
        Activity activity = activity(8L, 2L);
        Registration first = registration(21L);
        Registration second = registration(22L);
        when(activityMapper.selectById(8L)).thenReturn(activity);
        when(registrationMapper.selectList(any())).thenReturn(List.of(first, second));
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0, Notice.class);
            notice.setId(99L);
            return 1;
        });

        Notice created = noticeService.publishActivityNotice(8L, noticeRequest("all_registered"), organizer(2L));

        assertThat(created.getActivityId()).isEqualTo(8L);
        assertThat(created.getNoticeType()).isEqualTo("activity");
        assertThat(created.getTargetType()).isEqualTo("all_registered");
        ArgumentCaptor<NoticeReceiver> receiverCaptor = ArgumentCaptor.forClass(NoticeReceiver.class);
        verify(noticeReceiverMapper, org.mockito.Mockito.times(2)).insert(receiverCaptor.capture());
        assertThat(receiverCaptor.getAllValues()).extracting(NoticeReceiver::getReceiverId)
                .containsExactlyInAnyOrder(21L, 22L);
    }

    @Test
    void organizerCannotPublishNoticeForOtherOrganizerActivity() {
        when(activityMapper.selectById(8L)).thenReturn(activity(8L, 5L));

        assertThatThrownBy(() -> noticeService.publishActivityNotice(8L, noticeRequest("all_registered"), organizer(2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能给自己创建的活动发布通知");

        verify(noticeMapper, never()).insert(any(Notice.class));
    }

    @Test
    void marksOwnMessageAsRead() {
        NoticeReceiver receiver = new NoticeReceiver();
        receiver.setId(12L);
        receiver.setReceiverId(3L);
        receiver.setReadStatus(0);
        when(noticeReceiverMapper.selectById(12L)).thenReturn(receiver);

        noticeService.markRead(12L, student(3L));

        assertThat(receiver.getReadStatus()).isEqualTo(1);
        assertThat(receiver.getReadTime()).isNotNull();
        verify(noticeReceiverMapper).updateById(receiver);
    }

    @Test
    void registrationSuccessUsesSpecificNoticeTypeAndSingleStudentReceiver() {
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0, Notice.class);
            notice.setId(101L);
            return 1;
        });

        noticeService.notifyRegistrationSuccess(activity(8L, 2L), new Registration(), student(21L));

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getNoticeType()).isEqualTo("registration_success");
        assertThat(noticeCaptor.getValue().getTitle()).isEqualTo("报名成功");
        ArgumentCaptor<NoticeReceiver> receiverCaptor = ArgumentCaptor.forClass(NoticeReceiver.class);
        verify(noticeReceiverMapper).insert(receiverCaptor.capture());
        assertThat(receiverCaptor.getValue().getReceiverId()).isEqualTo(21L);
    }

    @Test
    void checkinSuccessCreatesSpecificNoticeForCheckedStudent() {
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0, Notice.class);
            notice.setId(102L);
            return 1;
        });
        Checkin checkin = new Checkin();
        checkin.setCheckinType("online");

        noticeService.notifyCheckinSuccess(activity(8L, 2L), student(21L), checkin);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getNoticeType()).isEqualTo("checkin_success");
        assertThat(noticeCaptor.getValue().getContent()).contains("线上签到成功");
    }

    @Test
    void activityStartReminderSkipsDuplicateReceiver() {
        Activity activity = activity(8L, 2L);
        activity.setStatus("PUBLISHED");
        activity.setStartTime(java.time.LocalDateTime.now().plusMinutes(25));
        Registration registration = registration(21L);
        Notice existing = new Notice();
        existing.setId(88L);
        existing.setActivityId(8L);
        existing.setNoticeType("activity_start_remind");

        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(registrationMapper.selectList(any())).thenReturn(List.of(registration));
        when(noticeMapper.selectList(any())).thenReturn(List.of(existing));
        when(noticeReceiverMapper.selectCount(any())).thenReturn(1L);

        int sent = noticeService.runActivityStartReminders();

        assertThat(sent).isZero();
        verify(noticeMapper, never()).insert(any(Notice.class));
    }

    @Test
    void feedbackReminderTargetsCheckedStudentsWithoutFeedback() {
        Activity activity = activity(8L, 2L);
        activity.setStatus("PUBLISHED");
        activity.setEndTime(java.time.LocalDateTime.now().minusMinutes(5));
        Checkin checkin = new Checkin();
        checkin.setUserId(21L);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(checkinMapper.selectList(any())).thenReturn(List.of(checkin));
        when(feedbackMapper.selectCount(any())).thenReturn(0L);
        when(noticeMapper.selectList(any())).thenReturn(List.of());
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0, Notice.class);
            notice.setId(103L);
            return 1;
        });

        int sent = noticeService.runFeedbackReminders();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getNoticeType()).isEqualTo("feedback_remind");
    }

    private NoticeRequest noticeRequest(String targetType) {
        NoticeRequest request = new NoticeRequest();
        request.setTitle("活动通知");
        request.setContent("请准时参加活动");
        request.setNoticeType("activity");
        request.setTargetType(targetType);
        return request;
    }

    private Activity activity(Long id, Long creatorId) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setCreatorId(creatorId);
        activity.setTitle("校园活动");
        return activity;
    }

    private Registration registration(Long userId) {
        Registration registration = new Registration();
        registration.setUserId(userId);
        return registration;
    }

    private User organizer(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("organizer");
        user.setUsername("organizer" + id);
        user.setRealName("负责人" + id);
        return user;
    }

    private User student(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        return user;
    }
}
