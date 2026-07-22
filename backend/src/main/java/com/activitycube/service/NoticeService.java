package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.NoticeRequest;
import com.activitycube.dto.SystemNoticeRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Notice;
import com.activitycube.entity.NoticeReceiver;
import com.activitycube.entity.Registration;
import com.activitycube.entity.StudentActivityReward;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.NoticeMapper;
import com.activitycube.mapper.NoticeReceiverMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.NoticeMessage;
import com.activitycube.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoticeService {
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_CHECKIN_REMINDER = "checkin_reminder";
    public static final String TYPE_FEEDBACK_REMINDER = "feedback_reminder";
    public static final String TYPE_REGISTRATION_SUCCESS = "registration_success";
    public static final String TYPE_CHECKIN_SUCCESS = "checkin_success";
    public static final String TYPE_ACTIVITY_START_REMIND = "activity_start_remind";
    public static final String TYPE_ACTIVITY_UPDATE = "activity_update";
    public static final String TYPE_CHECKIN_REMIND = "checkin_remind";
    public static final String TYPE_CHECKIN_DEADLINE_REMIND = "checkin_deadline_remind";
    public static final String TYPE_FEEDBACK_REMIND = "feedback_remind";
    public static final String TYPE_MANUAL_CHECKIN = "manual_checkin";
    public static final String TYPE_REWARD_ISSUED = "reward_issued";
    public static final String TYPE_SYSTEM = "system";
    public static final String TARGET_ALL_REGISTERED = "all_registered";
    public static final String TARGET_CHECKED_IN = "checked_in";
    public static final String TARGET_NOT_CHECKED_IN = "not_checked_in";

    private final NoticeMapper noticeMapper;
    private final NoticeReceiverMapper noticeReceiverMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    public PageResult<NoticeMessage> myMessages(User user, String type, Integer readStatus, long page, long size) {
        List<NoticeMessage> all = noticeReceiverMapper.selectList(new LambdaQueryWrapper<NoticeReceiver>()
                        .eq(NoticeReceiver::getReceiverId, user.getId())
                        .orderByDesc(NoticeReceiver::getCreatedAt))
                .stream()
                .map(this::toMessage)
                .filter(Objects::nonNull)
                .filter(message -> !StringUtils.hasText(type) || "all".equals(type) || type.equals(message.getNoticeType()))
                .filter(message -> readStatus == null || Objects.equals(message.getReadStatus(), readStatus))
                .toList();
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 100);
        int fromIndex = (int) Math.min((current - 1) * pageSize, all.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, all.size());
        return new PageResult<>(all.subList(fromIndex, toIndex), all.size(), current, pageSize);
    }

    public long unreadCount(User user) {
        return noticeReceiverMapper.selectCount(new LambdaQueryWrapper<NoticeReceiver>()
                .eq(NoticeReceiver::getReceiverId, user.getId())
                .eq(NoticeReceiver::getReadStatus, 0));
    }

    public void markRead(Long messageId, User user) {
        NoticeReceiver receiver = noticeReceiverMapper.selectById(messageId);
        if (receiver == null || !Objects.equals(receiver.getReceiverId(), user.getId())) {
            throw new BusinessException("消息不存在");
        }
        if (receiver.getReadStatus() != null && receiver.getReadStatus() == 1) {
            return;
        }
        receiver.setReadStatus(1);
        receiver.setReadTime(LocalDateTime.now());
        noticeReceiverMapper.updateById(receiver);
    }

    public void markAllRead(User user) {
        List<NoticeReceiver> unreadList = noticeReceiverMapper.selectList(new LambdaQueryWrapper<NoticeReceiver>()
                .eq(NoticeReceiver::getReceiverId, user.getId())
                .eq(NoticeReceiver::getReadStatus, 0));
        LocalDateTime now = LocalDateTime.now();
        unreadList.forEach(receiver -> {
            receiver.setReadStatus(1);
            receiver.setReadTime(now);
            noticeReceiverMapper.updateById(receiver);
        });
    }

    public List<Notice> activityNotices(Long activityId, User user) {
        requireManageableActivity(activityId, user);
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getActivityId, activityId)
                .orderByDesc(Notice::getCreatedAt));
    }

    @Transactional
    public Notice publishActivityNotice(Long activityId, NoticeRequest request, User sender) {
        Activity activity = requireManageableActivity(activityId, sender);
        String noticeType = normalizeNoticeType(request.getNoticeType(), false);
        String targetType = normalizeTargetType(request.getTargetType());
        Notice notice = createNotice(activityId, sender, request.getTitle(), request.getContent(), noticeType, targetType);
        createReceivers(notice.getId(), resolveActivityReceivers(activityId, targetType));
        return notice;
    }

    @Transactional
    public Notice publishSystemNotice(SystemNoticeRequest request, User sender) {
        AuthUtil.requireAdmin(sender);
        Notice notice = createNotice(null, sender, request.getTitle(), request.getContent(), TYPE_SYSTEM,
                StringUtils.hasText(request.getTargetType()) ? request.getTargetType() : "all_students");
        List<Long> receivers = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getRole, List.of("student", "user")))
                .stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();
        createReceivers(notice.getId(), receivers);
        return notice;
    }

    public void notifyRegistrationSuccess(Activity activity, Registration registration, User user) {
        if (activity == null || registration == null || user == null || user.getId() == null) {
            return;
        }
        Notice notice = createNotice(activity.getId(), null, "报名成功",
                "你已成功报名【" + activity.getTitle() + "】，请按时参加活动。", TYPE_REGISTRATION_SUCCESS, "single_user");
        createReceivers(notice.getId(), List.of(user.getId()));
    }

    public void notifyCheckinSuccess(Activity activity, User student, Checkin checkin) {
        if (activity == null || student == null || student.getId() == null || checkin == null) {
            return;
        }
        String methodText = checkinMethodText(checkin.getCheckinType());
        Notice notice = createNotice(activity.getId(), null, "签到成功",
                "你已完成【" + activity.getTitle() + "】" + methodText + "。", TYPE_CHECKIN_SUCCESS, "single_user");
        createReceivers(notice.getId(), List.of(student.getId()));
    }

    public void notifyActivityCancelled(Activity activity, User sender) {
        if (activity == null || activity.getId() == null) {
            return;
        }
        Notice notice = createNotice(activity.getId(), sender, "活动已取消",
                "【" + activity.getTitle() + "】已取消，请留意后续安排。", TYPE_ACTIVITY, TARGET_ALL_REGISTERED);
        createReceivers(notice.getId(), resolveActivityReceivers(activity.getId(), TARGET_ALL_REGISTERED));
    }

    public void notifyActivityUpdated(Activity activity, User sender, List<String> changedFields) {
        if (activity == null || activity.getId() == null) {
            return;
        }
        String content = "你报名的【" + activity.getTitle() + "】活动信息发生变化，请及时查看最新安排。";
        if (changedFields != null && !changedFields.isEmpty()) {
            content += "变更内容：" + String.join("、", changedFields) + "。";
        }
        if (sameNoticeExists(activity.getId(), TYPE_ACTIVITY_UPDATE, content)) {
            return;
        }
        Notice notice = createNotice(activity.getId(), sender, "活动信息已变更",
                content, TYPE_ACTIVITY_UPDATE, TARGET_ALL_REGISTERED);
        createReceivers(notice.getId(), resolveActivityReceivers(activity.getId(), TARGET_ALL_REGISTERED));
    }

    public void notifyManualCheckin(Activity activity, User student, User operator) {
        if (activity == null || student == null || student.getId() == null) {
            return;
        }
        Notice notice = createNotice(activity.getId(), operator, "签到成功",
                "你已完成【" + activity.getTitle() + "】人工补签成功。", TYPE_CHECKIN_SUCCESS, "single_user");
        createReceivers(notice.getId(), List.of(student.getId()));
    }

    public void notifyActivityRewardIssued(Activity activity, StudentActivityReward reward, User operator) {
        if (activity == null || reward == null || reward.getStudentId() == null) {
            return;
        }
        Notice notice = createNotice(activity.getId(), operator, "活动奖励已发放",
                "你在【" + activity.getTitle() + "】中的活动奖励已发放，可在个人中心查看。", TYPE_REWARD_ISSUED, "single_user");
        createReceivers(notice.getId(), List.of(reward.getStudentId()));
    }

    private String checkinMethodText(String checkinType) {
        if ("manual".equals(checkinType)) {
            return "人工补签成功";
        }
        if ("online".equals(checkinType)) {
            return "线上签到成功";
        }
        return "扫码签到成功";
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void runScheduledReminders() {
        runAllReminders();
    }

    @Transactional
    public int runAllReminders() {
        return runActivityStartReminders()
                + runCheckinStartReminders()
                + runCheckinDeadlineReminders()
                + runFeedbackReminders();
    }

    @Transactional
    public int runActivityStartReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindBefore = now.plusMinutes(30);
        int sent = 0;
        for (Activity activity : publishedActivities()) {
            LocalDateTime startTime = activity.getStartTime();
            if (startTime == null || startTime.isBefore(now) || startTime.isAfter(remindBefore)) {
                continue;
            }
            for (Registration registration : registrations(activity.getId())) {
                sent += createSingleReceiverNoticeIfAbsent(activity, registration.getUserId(), TYPE_ACTIVITY_START_REMIND,
                        "活动即将开始",
                        "你报名的【" + activity.getTitle() + "】将于 30 分钟后开始，请按时参加。");
            }
        }
        return sent;
    }

    @Transactional
    public int runCheckinStartReminders() {
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        for (Activity activity : publishedActivities()) {
            LocalDateTime start = activity.getCheckinStartTime() == null ? activity.getStartTime() : activity.getCheckinStartTime();
            LocalDateTime end = activity.getCheckinEndTime() == null ? activity.getEndTime() : activity.getCheckinEndTime();
            if (start == null || end == null || now.isBefore(start) || now.isAfter(end)) {
                continue;
            }
            Set<Long> checkedUserIds = checkedUserIds(activity.getId());
            for (Registration registration : registrations(activity.getId())) {
                if (checkedUserIds.contains(registration.getUserId())) {
                    continue;
                }
                sent += createSingleReceiverNoticeIfAbsent(activity, registration.getUserId(), TYPE_CHECKIN_REMIND,
                        "签到已开始",
                        "【" + activity.getTitle() + "】签到已开始，请及时完成签到。");
            }
        }
        return sent;
    }

    @Transactional
    public int runCheckinDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(10);
        int sent = 0;
        for (Activity activity : publishedActivities()) {
            LocalDateTime end = activity.getCheckinEndTime();
            if (end == null || end.isBefore(now) || end.isAfter(deadline)) {
                continue;
            }
            Set<Long> checkedUserIds = checkedUserIds(activity.getId());
            for (Registration registration : registrations(activity.getId())) {
                if (checkedUserIds.contains(registration.getUserId())) {
                    continue;
                }
                sent += createSingleReceiverNoticeIfAbsent(activity, registration.getUserId(), TYPE_CHECKIN_DEADLINE_REMIND,
                        "签到即将截止",
                        "【" + activity.getTitle() + "】签到即将截止，请尽快完成签到。");
            }
        }
        return sent;
    }

    @Transactional
    public int runFeedbackReminders() {
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        for (Activity activity : publishedActivities()) {
            if (activity.getEndTime() == null || activity.getEndTime().isAfter(now)) {
                continue;
            }
            for (Checkin checkin : checkins(activity.getId())) {
                if (hasFeedback(activity.getId(), checkin.getUserId())) {
                    continue;
                }
                sent += createSingleReceiverNoticeIfAbsent(activity, checkin.getUserId(), TYPE_FEEDBACK_REMIND,
                        "请填写活动反馈",
                        "【" + activity.getTitle() + "】已结束，欢迎填写活动反馈。");
            }
        }
        return sent;
    }

    private List<Activity> publishedActivities() {
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, ActivityStatusUtil.PUBLISHED));
    }

    private List<Registration> registrations(Long activityId) {
        return registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId));
    }

    private List<Checkin> checkins(Long activityId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getActivityId, activityId));
    }

    private Set<Long> checkedUserIds(Long activityId) {
        return checkins(activityId).stream()
                .map(Checkin::getUserId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean hasFeedback(Long activityId, Long userId) {
        return feedbackMapper.selectCount(new LambdaQueryWrapper<com.activitycube.entity.Feedback>()
                .eq(com.activitycube.entity.Feedback::getActivityId, activityId)
                .eq(com.activitycube.entity.Feedback::getUserId, userId)) > 0;
    }

    private int createSingleReceiverNoticeIfAbsent(Activity activity, Long receiverId, String noticeType, String title, String content) {
        if (activity == null || activity.getId() == null || receiverId == null) {
            return 0;
        }
        if (noticeExists(activity.getId(), receiverId, noticeType)) {
            return 0;
        }
        Notice notice = createNotice(activity.getId(), null, title, content, noticeType, "single_user");
        createReceivers(notice.getId(), List.of(receiverId));
        return 1;
    }

    private boolean noticeExists(Long activityId, Long receiverId, String noticeType) {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getActivityId, activityId)
                .eq(Notice::getNoticeType, noticeType));
        for (Notice notice : notices) {
            Long count = noticeReceiverMapper.selectCount(new LambdaQueryWrapper<NoticeReceiver>()
                    .eq(NoticeReceiver::getNoticeId, notice.getId())
                    .eq(NoticeReceiver::getReceiverId, receiverId));
            if (count != null && count > 0) {
                return true;
            }
        }
        return false;
    }

    private Notice createNotice(Long activityId, User sender, String title, String content, String noticeType, String targetType) {
        Notice notice = new Notice();
        notice.setActivityId(activityId);
        if (sender != null) {
            notice.setSenderId(sender.getId());
            notice.setSenderName(StringUtils.hasText(sender.getRealName()) ? sender.getRealName() : sender.getUsername());
        } else {
            notice.setSenderName("系统通知");
        }
        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setNoticeType(noticeType);
        notice.setTargetType(targetType);
        notice.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice;
    }

    private void createReceivers(Long noticeId, List<Long> receiverIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(receiverIds);
        LocalDateTime now = LocalDateTime.now();
        uniqueIds.stream()
                .filter(Objects::nonNull)
                .forEach(receiverId -> {
                    NoticeReceiver receiver = new NoticeReceiver();
                    receiver.setNoticeId(noticeId);
                    receiver.setReceiverId(receiverId);
                    receiver.setReadStatus(0);
                    receiver.setCreatedAt(now);
                    noticeReceiverMapper.insert(receiver);
                });
    }

    private List<Long> resolveActivityReceivers(Long activityId, String targetType) {
        List<Registration> registrations = registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId));
        if (TARGET_ALL_REGISTERED.equals(targetType)) {
            return registrations.stream().map(Registration::getUserId).toList();
        }
        Set<Long> checkedUserIds = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getActivityId, activityId))
                .stream()
                .map(Checkin::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        if (TARGET_CHECKED_IN.equals(targetType)) {
            return new ArrayList<>(checkedUserIds);
        }
        return registrations.stream()
                .map(Registration::getUserId)
                .filter(userId -> !checkedUserIds.contains(userId))
                .toList();
    }

    private Activity requireManageableActivity(Long activityId, User user) {
        AuthUtil.requireOrganizerOrAdmin(user);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (!"admin".equals(user.getRole()) && !Objects.equals(activity.getCreatorId(), user.getId())) {
            throw new BusinessException("只能给自己创建的活动发布通知");
        }
        return activity;
    }

    private NoticeMessage toMessage(NoticeReceiver receiver) {
        Notice notice = noticeMapper.selectById(receiver.getNoticeId());
        if (notice == null) {
            return null;
        }
        NoticeMessage message = new NoticeMessage();
        message.setId(receiver.getId());
        message.setNoticeId(notice.getId());
        message.setActivityId(notice.getActivityId());
        message.setActivityTitle(resolveActivityTitle(notice.getActivityId()));
        message.setSenderName(notice.getSenderName());
        message.setTitle(notice.getTitle());
        message.setContent(notice.getContent());
        message.setSummary(summary(notice.getContent()));
        message.setNoticeType(notice.getNoticeType());
        message.setTargetType(notice.getTargetType());
        message.setReadStatus(receiver.getReadStatus());
        message.setReadTime(receiver.getReadTime());
        message.setCreatedAt(receiver.getCreatedAt());
        return message;
    }

    private String resolveActivityTitle(Long activityId) {
        if (activityId == null) {
            return null;
        }
        Activity activity = activityMapper.selectById(activityId);
        return activity == null ? null : activity.getTitle();
    }

    private String summary(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= 60 ? content : content.substring(0, 60) + "...";
    }

    private String normalizeNoticeType(String noticeType, boolean systemOnly) {
        if (systemOnly) {
            return TYPE_SYSTEM;
        }
        if (List.of(TYPE_ACTIVITY,
                TYPE_CHECKIN_REMINDER,
                TYPE_FEEDBACK_REMINDER,
                TYPE_REGISTRATION_SUCCESS,
                TYPE_CHECKIN_SUCCESS,
                TYPE_ACTIVITY_START_REMIND,
                TYPE_ACTIVITY_UPDATE,
                TYPE_CHECKIN_REMIND,
                TYPE_CHECKIN_DEADLINE_REMIND,
                TYPE_FEEDBACK_REMIND,
                TYPE_MANUAL_CHECKIN,
                TYPE_REWARD_ISSUED).contains(noticeType)) {
            return noticeType;
        }
        throw new BusinessException("通知类型不正确");
    }

    private boolean sameNoticeExists(Long activityId, String noticeType, String content) {
        return noticeMapper.selectCount(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getActivityId, activityId)
                .eq(Notice::getNoticeType, noticeType)
                .eq(Notice::getContent, content)) > 0;
    }

    private String normalizeTargetType(String targetType) {
        if (List.of(TARGET_ALL_REGISTERED, TARGET_CHECKED_IN, TARGET_NOT_CHECKED_IN).contains(targetType)) {
            return targetType;
        }
        throw new BusinessException("通知对象不正确");
    }
}
