package com.activitycube.controller;

import com.activitycube.common.Result;
import com.activitycube.entity.User;
import com.activitycube.util.AuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/me")
    public Result<User> me() {
        User user = AuthUtil.requireUser();
        if ("user".equals(user.getRole())) {
            user.setRole("student");
        }
        user.setPassword(null);
        return Result.success(user);
    }
}
