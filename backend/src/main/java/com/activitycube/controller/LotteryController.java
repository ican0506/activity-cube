package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.LotteryDrawRequest;
import com.activitycube.entity.LotteryResult;
import com.activitycube.service.ActivityService;
import com.activitycube.service.LotteryService;
import com.activitycube.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities/{id}/lottery")
@RequiredArgsConstructor
public class LotteryController {
    private final ActivityService activityService;
    private final LotteryService lotteryService;

    @PostMapping("/draw")
    public Result<List<LotteryResult>> draw(@PathVariable Long id, @Valid @RequestBody LotteryDrawRequest request) {
        activityService.requireManageableActivity(id, AuthUtil.requireUser());
        return Result.success(lotteryService.draw(id, request));
    }

    @GetMapping("/results")
    public Result<List<LotteryResult>> results(@PathVariable Long id) {
        activityService.requireManageableActivity(id, AuthUtil.requireUser());
        return Result.success(lotteryService.results(id));
    }
}
