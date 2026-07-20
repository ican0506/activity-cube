package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.StudentPasswordRequest;
import com.activitycube.dto.StudentProfileUpdateRequest;
import com.activitycube.entity.User;
import com.activitycube.service.StudentProfileService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.StudentProfileSummary;
import com.activitycube.vo.StudentTodoItem;
import com.activitycube.vo.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StudentProfileController {
    private final StudentProfileService studentProfileService;

    @GetMapping("/api/student/profile")
    public Result<User> profile() {
        return Result.success(studentProfileService.profile(AuthUtil.requireUser()));
    }

    @PutMapping("/api/student/profile")
    public Result<User> updateProfile(@RequestBody StudentProfileUpdateRequest request) {
        return Result.success(studentProfileService.updateProfile(AuthUtil.requireUser(), request));
    }

    @PostMapping("/api/student/profile/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        FileUploadResult result = studentProfileService.uploadAvatar(AuthUtil.requireUser(), file);
        return new Result<>(200, "上传成功", Map.of("avatarUrl", result.getUrl()));
    }

    @GetMapping("/api/student/profile/summary")
    public Result<StudentProfileSummary> summary() {
        return Result.success(studentProfileService.summary(AuthUtil.requireUser()));
    }

    @GetMapping("/api/student/profile/todos")
    public Result<List<StudentTodoItem>> todos() {
        return Result.success(studentProfileService.todos(AuthUtil.requireUser()));
    }

    @PutMapping("/api/student/password")
    public Result<Void> changePassword(@RequestBody StudentPasswordRequest request) {
        studentProfileService.changePassword(AuthUtil.requireUser(), request);
        return Result.success();
    }
}
