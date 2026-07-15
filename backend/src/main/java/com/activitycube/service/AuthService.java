package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LoginRequest;
import com.activitycube.dto.RegisterUserRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
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
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }

    public LoginResponse register(RegisterUserRequest request) {
        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (existingCount > 0) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setRole("user");
        user.setCampus(request.getCampus());
        user.setCollege(request.getCollege());
        user.setMajorClass(request.getMajorClass());
        user.setPhone(request.getPhone());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }
}
