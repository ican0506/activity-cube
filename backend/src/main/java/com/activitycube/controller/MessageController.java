package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.NoticeService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.NoticeMessage;
import com.activitycube.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageController {
    private final NoticeService noticeService;

    @GetMapping("/api/messages")
    public Result<PageResult<NoticeMessage>> messages(@RequestParam(required = false) String type,
                                                       @RequestParam(required = false) Integer readStatus,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.success(noticeService.myMessages(AuthUtil.requireUser(), type, readStatus, page, size));
    }

    @GetMapping("/api/messages/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(noticeService.unreadCount(AuthUtil.requireUser()));
    }

    @PutMapping("/api/messages/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        noticeService.markRead(id, AuthUtil.requireUser());
        return Result.success();
    }

    @PutMapping("/api/messages/read-all")
    public Result<Void> readAll() {
        noticeService.markAllRead(AuthUtil.requireUser());
        return Result.success();
    }
}
