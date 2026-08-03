package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.ai.ActivityCopywritingRequest;
import com.activitycube.dto.ai.ActivityCopywritingResult;
import com.activitycube.dto.ai.ActivitySummaryResult;
import com.activitycube.service.ActivityAiService;
import com.activitycube.service.ActivitySummaryAiService;
import com.activitycube.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiController {
    private final ActivityAiService activityAiService;
    private final ActivitySummaryAiService activitySummaryAiService;

    @PostMapping("/api/ai/activity/copywriting")
    public Result<ActivityCopywritingResult> copywriting(@Valid @RequestBody ActivityCopywritingRequest request) {
        return Result.success(activityAiService.generate(request, AuthUtil.requireUser()));
    }

    @PostMapping("/api/ai/activity/{activityId}/summary")
    public Result<ActivitySummaryResult> activitySummary(@PathVariable Long activityId) {
        return Result.success(activitySummaryAiService.generateSummary(activityId, AuthUtil.requireUser()));
    }
}
