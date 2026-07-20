package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.UserResetPasswordRequest;
import com.activitycube.dto.UserRoleRequest;
import com.activitycube.dto.UserStatusRequest;
import com.activitycube.dto.UserUpdateRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.StudentNoUtil;
import com.activitycube.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final PasswordService passwordService;

    public PageResult<User> list(String keyword, String role, String campus, Integer status, long page, long size, User operator) {
        requireAdmin(operator);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .or().like(User::getWorkNo, keyword)
                    .or().like(User::getPhone, keyword));
        }
        String normalizedRole = normalizeRoleFilter(role);
        if (StringUtils.hasText(normalizedRole)) {
            if ("student".equals(normalizedRole)) {
                wrapper.in(User::getRole, List.of("student", "user"));
            } else {
                wrapper.eq(User::getRole, normalizedRole);
            }
        }
        if (StringUtils.hasText(campus) && !"全部".equals(campus)) {
            wrapper.eq(User::getCampus, campus);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 100);
        Page<User> result = userMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<User> records = result.getRecords().stream()
                .map(this::sanitize)
                .toList();
        return new PageResult<>(records, result.getTotal(), current, pageSize);
    }

    public User update(Long id, UserUpdateRequest request, User operator) {
        requireAdmin(operator);
        User user = requireUser(id);
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setWorkNo(request.getWorkNo());
        if (StringUtils.hasText(request.getStudentNo())) {
            StudentNoUtil.ParsedStudentNo parsed = StudentNoUtil.parse(request.getStudentNo());
            user.setGradeYear(parsed.gradeYear());
            user.setMajorCode(parsed.majorCode());
            user.setMajorName(StringUtils.hasText(parsed.majorName()) ? parsed.majorName() : request.getMajorName());
        } else {
            user.setGradeYear(request.getGradeYear());
            user.setMajorCode(request.getMajorCode());
            user.setMajorName(request.getMajorName());
        }
        user.setClassName(request.getClassName());
        if (StringUtils.hasText(request.getMajorName()) && StringUtils.hasText(request.getClassName())) {
            user.setMajorClass(request.getMajorName() + request.getClassName());
        }
        user.setPhone(request.getPhone());
        user.setCampus(request.getCampus());
        user.setCollege(request.getCollege());
        if (StringUtils.hasText(request.getRole())) {
            if (!"admin".equals(user.getRole()) || !"admin".equals(request.getRole())) {
                user.setRole(normalizeEditableRole(request.getRole()));
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return sanitize(user);
    }

    public User updateStatus(Long id, UserStatusRequest request, User operator) {
        requireAdmin(operator);
        User user = requireUser(id);
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException("账号状态只能是启用或禁用");
        }
        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        operationLogService.record(operator, "update_user_status", "user", id,
                (request.getStatus() == 1 ? "启用用户：" : "禁用用户：") + user.getUsername());
        return sanitize(user);
    }

    public void resetPassword(Long id, UserResetPasswordRequest request, User operator) {
        requireAdmin(operator);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("新密码不能为空");
        }
        User user = requireUser(id);
        user.setPassword(passwordService.encode(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public User updateRole(Long id, UserRoleRequest request, User operator) {
        requireAdmin(operator);
        User user = requireUser(id);
        user.setRole(normalizeEditableRole(request.getRole()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return sanitize(user);
    }

    private void requireAdmin(User operator) {
        try {
            AuthUtil.requireAdmin(operator);
        } catch (BusinessException exception) {
            throw new BusinessException("只有管理员可以管理用户");
        }
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String normalizeRoleFilter(String role) {
        if (!StringUtils.hasText(role) || "全部".equals(role)) {
            return "";
        }
        return "user".equals(role) ? "student" : role;
    }

    private String normalizeEditableRole(String role) {
        String normalized = "user".equals(role) ? "student" : role;
        if (!"student".equals(normalized) && !"organizer".equals(normalized)) {
            throw new BusinessException("用户管理页不允许创建或切换管理员角色");
        }
        return normalized;
    }

    private User sanitize(User user) {
        if ("user".equals(user.getRole())) {
            user.setRole("student");
        }
        user.setPassword(null);
        return user;
    }
}
