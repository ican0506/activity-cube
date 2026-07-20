package com.activitycube.controller;

import com.activitycube.service.ActivityService;
import com.activitycube.service.ExportService;
import com.activitycube.service.OperationLogService;
import com.activitycube.entity.User;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExportController {
    private final ActivityService activityService;
    private final ExportService exportService;
    private final OperationLogService operationLogService;

    @GetMapping("/api/activities/{id}/export/{type}")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @PathVariable String type) {
        User user = AuthUtil.requireUser();
        activityService.requireManageableActivity(id, user);
        byte[] data = exportService.export(id, type);
        operationLogService.record(user, "export_" + type, "activity", id, "导出" + exportTypeText(type));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    @PostMapping("/api/activities/{id}/export-log/{type}")
    public com.activitycube.common.Result<Void> recordFrontendExport(@PathVariable Long id, @PathVariable String type) {
        User user = AuthUtil.requireUser();
        activityService.requireManageableActivity(id, user);
        if (!"registrations".equals(type) && !"checkins".equals(type) && !"absences".equals(type)) {
            throw new com.activitycube.common.BusinessException("导出类型不正确");
        }
        operationLogService.record(user, "export_" + type, "activity", id, "前端导出" + exportTypeText(type));
        return com.activitycube.common.Result.success();
    }

    private String exportTypeText(String type) {
        return switch (type) {
            case "registrations" -> "报名名单";
            case "checkins" -> "签到名单";
            case "absences" -> "未签到名单";
            default -> "名单";
        };
    }
}
