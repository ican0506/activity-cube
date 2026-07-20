package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.entity.Activity;
import com.activitycube.service.ActivityService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QrCodeController {
    private final ActivityService activityService;

    @GetMapping("/api/admin/activities/{id}/qrcode")
    public Result<Activity> qrCodeActivity(@PathVariable Long id) {
        return Result.success(activityService.qrCodeActivity(id, AuthUtil.requireUser()));
    }

    @PostMapping("/api/admin/activities/{id}/checkin-code")
    public Result<Map<String, String>> generateCheckinCode(@PathVariable Long id) {
        String checkinCode = activityService.generateCheckinCode(id, AuthUtil.requireUser());
        return Result.success(Map.of("checkinCode", checkinCode));
    }
}
