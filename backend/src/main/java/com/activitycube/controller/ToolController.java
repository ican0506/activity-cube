package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.DrawRequest;
import com.activitycube.dto.GroupRequest;
import com.activitycube.entity.Registration;
import com.activitycube.service.ActivityService;
import com.activitycube.service.ToolService;
import com.activitycube.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ToolController {
    private final ActivityService activityService;
    private final ToolService toolService;

    @PostMapping("/api/activities/{id}/tools/draw")
    public Result<List<Registration>> draw(@PathVariable Long id, @Valid @RequestBody DrawRequest request) {
        activityService.requireManageableActivity(id, AuthUtil.requireUser());
        return Result.success(toolService.draw(id, request));
    }

    @PostMapping("/api/activities/{id}/tools/group")
    public Result<List<List<Registration>>> group(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        activityService.requireManageableActivity(id, AuthUtil.requireUser());
        return Result.success(toolService.group(id, request));
    }
}
