package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.StudentPasswordRequest;
import com.activitycube.dto.StudentProfileUpdateRequest;
import com.activitycube.entity.Activity;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Feedback;
import com.activitycube.entity.NoticeReceiver;
import com.activitycube.entity.Registration;
import com.activitycube.entity.StudentActivityReward;
import com.activitycube.entity.User;
import com.activitycube.mapper.ActivityMapper;
import com.activitycube.mapper.CheckinMapper;
import com.activitycube.mapper.FeedbackMapper;
import com.activitycube.mapper.NoticeReceiverMapper;
import com.activitycube.mapper.RegistrationMapper;
import com.activitycube.mapper.StudentActivityRewardMapper;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.ActivityStatusUtil;
import com.activitycube.vo.StudentProfileSummary;
import com.activitycube.vo.StudentTodoItem;
import com.activitycube.vo.FileUploadResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentProfileService {
    private final UserMapper userMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final FeedbackMapper feedbackMapper;
    private final ActivityMapper activityMapper;
    private final StudentActivityRewardMapper rewardMapper;
    private final NoticeReceiverMapper noticeReceiverMapper;
    private final FileService fileService;
    private final PasswordService passwordService;

    public User profile(User currentUser) {
        requireStudent(currentUser);
        User user = userMapper.selectById(currentUser.getId());
        if (user == null) {
            throw new BusinessException("学生信息不存在");
        }
        sanitize(user);
        return user;
    }

    @Transactional
    public User updateProfile(User currentUser, StudentProfileUpdateRequest request) {
        requireStudent(currentUser);
        if (request == null) {
            request = new StudentProfileUpdateRequest();
        }
        User patch = new User();
        patch.setId(currentUser.getId());
        patch.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        patch.setPhone(trimToNull(request.getPhone()));
        patch.setBio(trimToNull(request.getBio()));
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);

        User updated = userMapper.selectById(currentUser.getId());
        if (updated == null) {
            updated = currentUser;
            updated.setAvatarUrl(patch.getAvatarUrl());
            updated.setPhone(patch.getPhone());
            updated.setBio(patch.getBio());
        }
        sanitize(updated);
        return updated;
    }

    public StudentProfileSummary summary(User currentUser) {
        requireStudent(currentUser);
        StudentProfileSummary summary = new StudentProfileSummary();
        Long userId = currentUser.getId();
        summary.setRegistrationCount(countRegistrations(userId));
        summary.setCheckinCount(countCheckins(userId));
        summary.setPendingFeedbackCount(countPendingFeedbacks(userId));
        summary.setUnreadMessageCount(countUnreadMessages(userId));
        applyRewardSummary(summary, userId);
        return summary;
    }

    @Transactional
    public FileUploadResult uploadAvatar(User currentUser, MultipartFile file) {
        requireStudent(currentUser);
        FileUploadResult result = fileService.uploadAvatar(file);
        User patch = new User();
        patch.setId(currentUser.getId());
        patch.setAvatarUrl(result.getUrl());
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);
        return result;
    }

    public List<StudentTodoItem> todos(User currentUser) {
        requireStudent(currentUser);
        List<StudentTodoItem> items = new ArrayList<>();
        Long userId = currentUser.getId();
        LocalDateTime now = LocalDateTime.now();
        long unread = countUnreadMessages(userId);
        if (unread > 0) {
            items.add(todo("message", "你有未读消息", unread + " 条活动通知待查看", "/messages", null, null, "high"));
        }

        for (Registration registration : safeList(registrations(userId))) {
            Activity activity = activityMapper.selectById(registration.getActivityId());
            if (activity == null) {
                continue;
            }
            String status = ActivityStatusUtil.calculateStatus(activity, now);
            boolean checkedIn = hasCheckin(userId, activity.getId());
            boolean feedbackSubmitted = hasFeedback(userId, activity.getId());
            if (ActivityStatusUtil.WAITING_START.equals(status) && isWithinHours(now, activity.getStartTime(), 48)) {
                items.add(todo("upcoming", "活动即将开始", activity.getTitle() + " 即将开始，请留意时间和地点",
                        "/activities/" + activity.getId(), activity.getId(), activity.getStartTime(), "normal"));
            }
            if (ActivityStatusUtil.REGISTERING.equals(status) && isWithinHours(now, activity.getRegisterEndTime(), 24)) {
                items.add(todo("registration_closing", "报名即将截止", activity.getTitle() + " 报名即将截止，请确认报名信息",
                        "/activities/" + activity.getId(), activity.getId(), activity.getRegisterEndTime(), "normal"));
            }
            if (ActivityStatusUtil.ONGOING.equals(status) && !checkedIn) {
                items.add(todo("checkin", "待签到活动", activity.getTitle() + " 正在进行，请按要求完成签到",
                        "/activities/" + activity.getId(), activity.getId(), activity.getCheckinEndTime(), "high"));
            }
            if (ActivityStatusUtil.ENDED.equals(status) && !feedbackSubmitted) {
                items.add(todo("feedback", "待提交反馈", activity.getTitle() + " 已结束，欢迎提交活动反馈",
                        "/activities/" + activity.getId() + "/feedback", activity.getId(), activity.getEndTime(), "normal"));
            }
        }
        items.sort(Comparator.comparing(StudentTodoItem::getDueTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Transactional
    public void changePassword(User currentUser, StudentPasswordRequest request) {
        requireStudent(currentUser);
        if (request == null || !StringUtils.hasText(request.getOldPassword())) {
            throw new BusinessException("原密码不能为空");
        }
        if (!passwordService.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!StringUtils.hasText(request.getNewPassword()) || request.getNewPassword().length() < 6) {
            throw new BusinessException("新密码不能少于6位");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        User patch = new User();
        patch.setId(currentUser.getId());
        patch.setPassword(passwordService.encode(request.getNewPassword()));
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);
    }

    private void requireStudent(User user) {
        if (user == null || (!"student".equals(user.getRole()) && !"user".equals(user.getRole()))) {
            throw new BusinessException("只有学生可以访问个人中心");
        }
    }

    private Long countRegistrations(Long userId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId));
    }

    private Long countCheckins(Long userId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId));
    }

    private Long countUnreadMessages(Long userId) {
        return noticeReceiverMapper.selectCount(new LambdaQueryWrapper<NoticeReceiver>()
                .eq(NoticeReceiver::getReceiverId, userId)
                .eq(NoticeReceiver::getReadStatus, 0));
    }

    private Long countPendingFeedbacks(Long userId) {
        long count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Registration registration : safeList(registrations(userId))) {
            Activity activity = activityMapper.selectById(registration.getActivityId());
            if (activity != null
                    && ActivityStatusUtil.ENDED.equals(ActivityStatusUtil.calculateStatus(activity, now))
                    && !hasFeedback(userId, activity.getId())) {
                count++;
            }
        }
        return count;
    }

    private void applyRewardSummary(StudentProfileSummary summary, Long userId) {
        BigDecimal hours = BigDecimal.ZERO;
        int points = 0;
        for (StudentActivityReward reward : safeList(rewardMapper.selectList(new LambdaQueryWrapper<StudentActivityReward>()
                .eq(StudentActivityReward::getStudentId, userId)))) {
            if (reward.getRewardHours() != null) {
                hours = hours.add(reward.getRewardHours());
            }
            if (reward.getRewardPoints() != null) {
                points += reward.getRewardPoints();
            }
        }
        summary.setRewardHours(hours);
        summary.setRewardPoints(points);
    }

    private List<Registration> registrations(Long userId) {
        return registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId));
    }

    private boolean hasCheckin(Long userId, Long activityId) {
        return checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getActivityId, activityId)) > 0;
    }

    private boolean hasFeedback(Long userId, Long activityId) {
        return feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .eq(Feedback::getActivityId, activityId)) > 0;
    }

    private boolean isWithinHours(LocalDateTime now, LocalDateTime target, long hours) {
        return target != null && !target.isBefore(now) && !target.isAfter(now.plusHours(hours));
    }

    private StudentTodoItem todo(String type, String title, String description, String targetPath,
                                 Long activityId, LocalDateTime dueTime, String priority) {
        StudentTodoItem item = new StudentTodoItem();
        item.setType(type);
        item.setTitle(title);
        item.setDescription(description);
        item.setTargetPath(targetPath);
        item.setActivityId(activityId);
        item.setDueTime(dueTime);
        item.setPriority(priority);
        return item;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void sanitize(User user) {
        user.setPassword(null);
        if ("user".equals(user.getRole())) {
            user.setRole("student");
        }
    }
}
