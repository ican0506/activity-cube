package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.ActivityService;
import com.activitycube.service.StatService;
import com.activitycube.vo.ActivityStats;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;
    private final ActivityService activityService;

    @GetMapping("/api/activities/{id}/stats")
    public Result<ActivityStats> activityStats(@PathVariable Long id) {
        return Result.success(statService.activityStats(id));
    }

    @GetMapping("/api/admin/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("activityCount", activityService.list(null, null, null).size());
        dashboard.put("message", "活动魔方 MVP 数据概览");
        return Result.success(dashboard);
    }
}
