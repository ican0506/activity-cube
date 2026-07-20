package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.ManualCheckinRequest;
import com.activitycube.entity.Checkin;
import com.activitycube.entity.Registration;
import com.activitycube.service.CheckinService;
import com.activitycube.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CheckinController {
    private final CheckinService checkinService;

    @PostMapping("/api/activities/{id}/checkin")
    public Result<Checkin> checkin(@PathVariable Long id, @RequestParam(required = false) String code) {
        return Result.success(checkinService.checkin(id, AuthUtil.requireUser(), code));
    }

    @PostMapping("/api/activities/{id}/checkins/manual")
    public Result<Checkin> manualCheckin(@PathVariable Long id, @RequestBody ManualCheckinRequest request) {
        Result<Checkin> result = Result.success(checkinService.manualCheckin(id, request, AuthUtil.requireUser()));
        result.setMessage("补签成功");
        return result;
    }

    @GetMapping("/api/activities/{id}/checkins")
    public Result<List<Checkin>> checkins(@PathVariable Long id) {
        return Result.success(checkinService.listByActivity(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/activities/{id}/absences")
    public Result<List<Registration>> absences(@PathVariable Long id) {
        return Result.success(checkinService.absences(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/activities/{id}/absentees")
    public Result<List<Registration>> absentees(@PathVariable Long id) {
        return Result.success(checkinService.absences(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/my/checkins")
    public Result<List<Checkin>> myCheckins() {
        return Result.success(checkinService.myCheckins(AuthUtil.requireUser()));
    }
}
