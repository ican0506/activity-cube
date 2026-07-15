package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.dto.RegisterRequest;
import com.activitycube.entity.Registration;
import com.activitycube.service.RegistrationService;
import com.activitycube.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping("/api/activities/{id}/register")
    public Result<Registration> register(@PathVariable Long id, @Valid @RequestBody RegisterRequest request) {
        return Result.success(registrationService.register(id, request, AuthUtil.requireUser()));
    }

    @GetMapping("/api/activities/{id}/registrations")
    public Result<List<Registration>> registrations(@PathVariable Long id) {
        return Result.success(registrationService.listByActivity(id, AuthUtil.requireUser()));
    }

    @GetMapping("/api/my/registrations")
    public Result<List<Registration>> myRegistrations() {
        return Result.success(registrationService.myRegistrations(AuthUtil.requireUser()));
    }
}
