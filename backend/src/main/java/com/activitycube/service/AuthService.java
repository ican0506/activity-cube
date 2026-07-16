package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LoginRequest;
import com.activitycube.dto.OrganizerCreateRequest;
import com.activitycube.dto.RegisterUserRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.TokenUtil;
import com.activitycube.vo.LoginResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已禁用，请联系管理员");
        }
        normalizeStudentRole(user);
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }

    public LoginResponse register(RegisterUserRequest request) {
        ensureUsernameAvailable(request.getUsername());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setRole("student");
        user.setCampus(request.getCampus());
        user.setCollege(request.getCollege());
        user.setMajorClass(request.getMajorClass());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        normalizeStudentRole(user);
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }

    public User createOrganizer(OrganizerCreateRequest request, User operator) {
        AuthUtil.requireAdmin(operator);
        ensureUsernameAvailable(request.getUsername());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setRole("organizer");
        user.setCampus(request.getCampus());
        user.setCollege(request.getCollege());
        user.setMajorClass("活动负责人");
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    private void normalizeStudentRole(User user) {
        if ("user".equals(user.getRole())) {
            user.setRole("student");
        }
    }

    private void ensureUsernameAvailable(String username) {
        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (existingCount > 0) {
            throw new BusinessException("账号已存在");
        }
    }
}
