package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.ActivityRequest;
import com.activitycube.entity.Activity;
import com.activitycube.service.ActivityService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.ActivityDetail;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    public Result<List<Activity>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String campus,
                                       @RequestParam(required = false) String status) {
        return Result.success(activityService.list(keyword, campus, status));
    }

    @GetMapping("/{id}")
    public Result<ActivityDetail> detail(@PathVariable Long id) {
        return Result.success(activityService.detail(id));
    }

    @PostMapping
    public Result<Activity> create(@Valid @RequestBody ActivityRequest request) {
        return Result.success(activityService.create(request, AuthUtil.requireUser()));
    }

    @PutMapping("/{id}")
    public Result<Activity> update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return Result.success(activityService.update(id, request, AuthUtil.requireUser()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.delete(id, AuthUtil.requireUser());
        return Result.success();
    }
}
