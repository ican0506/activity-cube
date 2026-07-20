package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LoginRequest;
import com.activitycube.dto.OrganizerCreateRequest;
import com.activitycube.dto.RegisterUserRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.AuthUtil;
import com.activitycube.util.StudentNoUtil;
import com.activitycube.util.TokenUtil;
import com.activitycube.vo.LoginResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final PasswordService passwordService;

    public LoginResponse login(LoginRequest request) {
        String identifier = request.resolveIdentifier();
        if (identifier.isBlank() || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("账号和密码不能为空");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", identifier)
                .or()
                .eq("student_no", identifier)
                .or()
                .eq("work_no", identifier));
        if (user == null || !passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已禁用，请联系管理员");
        }
        upgradeLegacyPlaintextPassword(user, request.getPassword());
        normalizeStudentRole(user);
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }

    public LoginResponse register(RegisterUserRequest request) {
        String username = request.getStudentNo().trim();
        StudentNoUtil.ParsedStudentNo parsedStudentNo = StudentNoUtil.parse(username);
        ensureUsernameAvailable(username);
        ensureStudentNoAvailable(username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordService.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentNo(username);
        user.setWorkNo(null);
        user.setGradeYear(parsedStudentNo.gradeYear());
        user.setMajorCode(parsedStudentNo.majorCode());
        user.setMajorName(resolveMajorName(request.getMajorName(), parsedStudentNo));
        user.setRole("student");
        user.setCampus(request.getCampus());
        user.setCollege(request.getCollege());
        user.setClassName(request.getClassName());
        user.setMajorClass(resolveLegacyMajorClass(request));
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        normalizeStudentRole(user);
        user.setPassword(null);
        return new LoginResponse(TokenUtil.createToken(user.getId()), user);
    }

    @Transactional
    public User createOrganizer(OrganizerCreateRequest request, User operator) {
        AuthUtil.requireAdmin(operator);
        String workNo = request.getWorkNo().trim();
        ensureUsernameAvailable(workNo);
        ensureWorkNoAvailable(workNo);
        User user = new User();
        user.setUsername(workNo);
        user.setPassword(passwordService.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentNo(null);
        user.setWorkNo(workNo);
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
        operationLogService.record(operator, "create_organizer", "user", user.getId(), "创建活动负责人账号：" + user.getUsername());
        return user;
    }

    private void normalizeStudentRole(User user) {
        if ("user".equals(user.getRole())) {
            user.setRole("student");
        }
    }

    private void upgradeLegacyPlaintextPassword(User user, String rawPassword) {
        if (!passwordService.isBcrypt(user.getPassword())) {
            user.setPassword(passwordService.encode(rawPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    private void ensureUsernameAvailable(String username) {
        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (existingCount > 0) {
            throw new BusinessException("账号已存在");
        }
    }

    private void ensureStudentNoAvailable(String studentNo) {
        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo));
        if (existingCount > 0) {
            throw new BusinessException("学号已注册");
        }
    }

    private void ensureWorkNoAvailable(String workNo) {
        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getWorkNo, workNo));
        if (existingCount > 0) {
            throw new BusinessException("工号已存在");
        }
    }

    private String resolveMajorName(String submittedMajorName, StudentNoUtil.ParsedStudentNo parsedStudentNo) {
        if (parsedStudentNo.majorName() != null) {
            return parsedStudentNo.majorName();
        }
        return submittedMajorName;
    }

    private String resolveLegacyMajorClass(RegisterUserRequest request) {
        if (request.getMajorClass() != null && !request.getMajorClass().isBlank()) {
            return request.getMajorClass();
        }
        if (request.getMajorName() == null || request.getClassName() == null) {
            return request.getClassName();
        }
        return request.getMajorName() + request.getClassName();
    }
}
