package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationReminderController {
    private final NoticeService noticeService;

    @PostMapping("/api/admin/notifications/activity-start-reminders/run")
    public Result<Integer> runActivityStartReminders() {
        return Result.success(noticeService.runActivityStartReminders());
    }

    @PostMapping("/api/admin/notifications/checkin-reminders/run")
    public Result<Integer> runCheckinStartReminders() {
        return Result.success(noticeService.runCheckinStartReminders());
    }

    @PostMapping("/api/admin/notifications/checkin-deadline-reminders/run")
    public Result<Integer> runCheckinDeadlineReminders() {
        return Result.success(noticeService.runCheckinDeadlineReminders());
    }

    @PostMapping("/api/admin/notifications/feedback-reminders/run")
    public Result<Integer> runFeedbackReminders() {
        return Result.success(noticeService.runFeedbackReminders());
    }

    @PostMapping("/api/admin/notifications/reminders/run")
    public Result<Integer> runAllReminders() {
        return Result.success(noticeService.runAllReminders());
    }
}
