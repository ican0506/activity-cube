package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.NoticeRequest;
import com.activitycube.dto.SystemNoticeRequest;
import com.activitycube.entity.Notice;
import com.activitycube.service.NoticeService;
import com.activitycube.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping("/api/admin/activities/{id}/notices")
    public Result<List<Notice>> activityNotices(@PathVariable Long id) {
        return Result.success(noticeService.activityNotices(id, AuthUtil.requireUser()));
    }

    @PostMapping("/api/admin/activities/{id}/notices")
    public Result<Notice> publishActivityNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return Result.success(noticeService.publishActivityNotice(id, request, AuthUtil.requireUser()));
    }

    @PostMapping("/api/admin/notices/system")
    public Result<Notice> publishSystemNotice(@Valid @RequestBody SystemNoticeRequest request) {
        return Result.success(noticeService.publishSystemNotice(request, AuthUtil.requireUser()));
    }
}
