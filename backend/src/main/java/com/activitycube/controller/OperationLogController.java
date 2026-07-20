package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.entity.OperationLog;
import com.activitycube.service.OperationLogService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService operationLogService;

    @GetMapping("/api/admin/operation-logs")
    public Result<PageResult<OperationLog>> list(@RequestParam(required = false) String operation,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        return Result.success(operationLogService.list(operation, username, startTime, endTime, page, size, AuthUtil.requireUser()));
    }
}
