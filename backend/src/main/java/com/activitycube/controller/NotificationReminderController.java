package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.NoticeService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationReminderController {
    private final NoticeService noticeService;

    @PostMapping("/api/admin/notifications/activity-start-reminders/run")
    public Result<Integer> runActivityStartReminders() {
        AuthUtil.requireAdmin(AuthUtil.requireUser());
        return Result.success(noticeService.runActivityStartReminders());
    }

    @PostMapping("/api/admin/notifications/checkin-reminders/run")
    public Result<Integer> runCheckinStartReminders() {
        AuthUtil.requireAdmin(AuthUtil.requireUser());
        return Result.success(noticeService.runCheckinStartReminders());
    }

    @PostMapping("/api/admin/notifications/checkin-deadline-reminders/run")
    public Result<Integer> runCheckinDeadlineReminders() {
        AuthUtil.requireAdmin(AuthUtil.requireUser());
        return Result.success(noticeService.runCheckinDeadlineReminders());
    }

    @PostMapping("/api/admin/notifications/feedback-reminders/run")
    public Result<Integer> runFeedbackReminders() {
        AuthUtil.requireAdmin(AuthUtil.requireUser());
        return Result.success(noticeService.runFeedbackReminders());
    }

    @PostMapping("/api/admin/notifications/reminders/run")
    public Result<Integer> runAllReminders() {
        AuthUtil.requireAdmin(AuthUtil.requireUser());
        return Result.success(noticeService.runAllReminders());
    }
}
