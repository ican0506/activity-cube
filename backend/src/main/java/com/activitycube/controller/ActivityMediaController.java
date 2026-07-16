package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.ActivityMediaRequest;
import com.activitycube.entity.ActivityMedia;
import com.activitycube.service.ActivityMediaService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityMediaController {
    private final ActivityMediaService activityMediaService;

    @GetMapping("/api/activities/{id}/media")
    public Result<List<ActivityMedia>> list(@PathVariable Long id) {
        return Result.success(activityMediaService.listByActivity(id));
    }

    @PostMapping("/api/activities/{id}/media")
    public Result<List<ActivityMedia>> save(@PathVariable Long id, @RequestBody ActivityMediaRequest request) {
        return Result.success(activityMediaService.saveAll(id, request, AuthUtil.requireUser()));
    }

    @DeleteMapping("/api/activities/media/{mediaId}")
    public Result<Void> delete(@PathVariable Long mediaId) {
        activityMediaService.delete(mediaId, AuthUtil.requireUser());
        return Result.success();
    }
}
