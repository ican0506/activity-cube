package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.FeedbackRequest;
import com.activitycube.entity.Feedback;
import com.activitycube.service.FeedbackService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.FeedbackStats;
import com.activitycube.vo.FeedbackView;
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
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping("/api/activities/{id}/feedback")
    public Result<Feedback> submit(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
        return Result.success(feedbackService.submit(id, request, AuthUtil.requireUser()));
    }

    @GetMapping("/api/activities/{id}/feedbacks")
    public Result<List<FeedbackView>> feedbacks(@PathVariable Long id) {
        return Result.success(feedbackService.listByActivity(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/activities/{id}/feedback-stats")
    public Result<FeedbackStats> stats(@PathVariable Long id) {
        return Result.success(feedbackService.stats(id, AuthUtil.requireUser()));
    }
}
