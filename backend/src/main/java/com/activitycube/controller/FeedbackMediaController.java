package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.FeedbackMediaRequest;
import com.activitycube.entity.FeedbackMedia;
import com.activitycube.service.FeedbackMediaService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedbackMediaController {
    private final FeedbackMediaService feedbackMediaService;

    @GetMapping("/api/feedbacks/{feedbackId}/media")
    public Result<List<FeedbackMedia>> list(@PathVariable Long feedbackId) {
        AuthUtil.requireUser();
        return Result.success(feedbackMediaService.listByFeedback(feedbackId));
    }

    @PostMapping("/api/feedbacks/{feedbackId}/media")
    public Result<List<FeedbackMedia>> save(@PathVariable Long feedbackId, @RequestBody FeedbackMediaRequest request) {
        AuthUtil.requireUser();
        return Result.success(feedbackMediaService.saveAll(feedbackId, request));
    }
}
