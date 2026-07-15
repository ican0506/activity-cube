package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LoginRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.TokenUtil;
import com.activitycube.vo.LoginResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
