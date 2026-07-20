package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.RewardIssueRequest;
import com.activitycube.entity.StudentActivityReward;
import com.activitycube.service.ActivityRewardService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.StudentRewardView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityRewardController {
    private final ActivityRewardService activityRewardService;

    @PostMapping("/api/admin/activities/{id}/rewards/issue")
    public Result<List<StudentActivityReward>> issue(@PathVariable Long id, @RequestBody(required = false) RewardIssueRequest request) {
        return Result.success(activityRewardService.issueRewards(id, request, AuthUtil.requireUser()));
    }

    @GetMapping("/api/admin/activities/{id}/rewards")
    public Result<List<StudentActivityReward>> listByActivity(@PathVariable Long id) {
        return Result.success(activityRewardService.listByActivity(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/student/rewards")
    public Result<List<StudentRewardView>> myRewards() {
        return Result.success(activityRewardService.myRewards(AuthUtil.requireUser()));
    }
}
