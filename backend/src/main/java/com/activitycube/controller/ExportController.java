package com.activitycube.controller;

import com.activitycube.service.ActivityService;
import com.activitycube.service.ExportService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExportController {
    private final ActivityService activityService;
    private final ExportService exportService;

    @GetMapping("/api/activities/{id}/export/{type}")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @PathVariable String type) {
        activityService.requireManageableActivity(id, AuthUtil.requireUser());
        byte[] data = exportService.export(id, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }
}
