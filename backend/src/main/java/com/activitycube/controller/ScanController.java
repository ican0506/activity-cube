package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.service.ScanService;
import com.activitycube.vo.CheckinCodeResolveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScanController {
    private final ScanService scanService;

    @GetMapping("/api/scan/checkin-code/resolve")
    public Result<CheckinCodeResolveResult> resolveCheckinCode(@RequestParam String code) {
        return Result.success(scanService.resolveCheckinCode(code));
    }
}
