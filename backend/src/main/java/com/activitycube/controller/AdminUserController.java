package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.OrganizerCreateRequest;
import com.activitycube.dto.UserResetPasswordRequest;
import com.activitycube.dto.UserRoleRequest;
import com.activitycube.dto.UserStatusRequest;
import com.activitycube.dto.UserUpdateRequest;
import com.activitycube.entity.User;
import com.activitycube.service.AdminUserService;
import com.activitycube.service.AuthService;
import com.activitycube.util.AuthUtil;
import com.activitycube.vo.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserController {
    private final AuthService authService;
    private final AdminUserService adminUserService;

    @GetMapping("/api/admin/users")
    public Result<PageResult<User>> users(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String role,
                                          @RequestParam(required = false) String campus,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminUserService.list(keyword, role, campus, status, page, size, AuthUtil.requireUser()));
    }

    @PostMapping("/api/admin/users/organizers")
    public Result<User> createOrganizer(@Valid @RequestBody OrganizerCreateRequest request) {
        return Result.success(authService.createOrganizer(request, AuthUtil.requireUser()));
    }

    @PutMapping("/api/admin/users/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(adminUserService.update(id, request, AuthUtil.requireUser()));
    }

    @PutMapping("/api/admin/users/{id}/status")
    public Result<User> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return Result.success(adminUserService.updateStatus(id, request, AuthUtil.requireUser()));
    }

    @PutMapping("/api/admin/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserResetPasswordRequest request) {
        adminUserService.resetPassword(id, request, AuthUtil.requireUser());
        return Result.success();
    }

    @PutMapping("/api/admin/users/{id}/role")
    public Result<User> updateRole(@PathVariable Long id, @Valid @RequestBody UserRoleRequest request) {
        return Result.success(adminUserService.updateRole(id, request, AuthUtil.requireUser()));
    }
}
