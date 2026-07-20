package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.UserResetPasswordRequest;
import com.activitycube.dto.UserRoleRequest;
import com.activitycube.dto.UserStatusRequest;
import com.activitycube.dto.UserUpdateRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final PasswordService passwordService = new PasswordService();
    private final AdminUserService adminUserService = new AdminUserService(userMapper, operationLogService, passwordService);

    @Test
    void rejectsListByNonAdmin() {
        assertThatThrownBy(() -> adminUserService.list("", "", "", null, 1, 10, organizer()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有管理员可以管理用户");
    }

    @Test
    void updatesUserProfileAndNormalizesStudentRole() {
        User existing = student();
        when(userMapper.selectById(9L)).thenReturn(existing);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRealName("新姓名");
        request.setStudentNo("2321241389");
        request.setPhone("13800000000");
        request.setCampus("文化路校区");
        request.setCollege("信息工程学院");
        request.setMajorName("软件工程");
        request.setClassName("软件工程2301");
        request.setRole("student");

        User updated = adminUserService.update(9L, request, admin());

        assertThat(updated.getPassword()).isNull();
        assertThat(updated.getRealName()).isEqualTo("新姓名");
        assertThat(updated.getGradeYear()).isEqualTo("2023级");
        assertThat(updated.getMajorCode()).isEqualTo("21241");
        assertThat(updated.getMajorName()).isEqualTo("软件工程");
        assertThat(updated.getClassName()).isEqualTo("软件工程2301");
        assertThat(updated.getRole()).isEqualTo("student");
        verify(userMapper).updateById(existing);
    }

    @Test
    void togglesUserStatus() {
        User existing = student();
        when(userMapper.selectById(9L)).thenReturn(existing);
        UserStatusRequest request = new UserStatusRequest();
        request.setStatus(0);

        User updated = adminUserService.updateStatus(9L, request, admin());

        assertThat(updated.getStatus()).isZero();
        assertThat(updated.getPassword()).isNull();
        verify(userMapper).updateById(existing);
    }

    @Test
    void resetsUserPassword() {
        User existing = student();
        when(userMapper.selectById(9L)).thenReturn(existing);
        UserResetPasswordRequest request = new UserResetPasswordRequest();
        request.setPassword("new123456");

        adminUserService.resetPassword(9L, request, admin());

        assertThat(existing.getPassword()).isNotEqualTo("new123456");
        assertThat(passwordService.matches("new123456", existing.getPassword())).isTrue();
        verify(userMapper).updateById(existing);
    }

    @Test
    void changesRoleBetweenStudentAndOrganizer() {
        User existing = student();
        when(userMapper.selectById(9L)).thenReturn(existing);
        UserRoleRequest request = new UserRoleRequest();
        request.setRole("organizer");

        User updated = adminUserService.updateRole(9L, request, admin());

        assertThat(updated.getRole()).isEqualTo("organizer");
        assertThat(updated.getPassword()).isNull();
        verify(userMapper).updateById(existing);
    }

    @Test
    void rejectsChangingUserToAdminFromManagementPage() {
        when(userMapper.selectById(9L)).thenReturn(student());
        UserRoleRequest request = new UserRoleRequest();
        request.setRole("admin");

        assertThatThrownBy(() -> adminUserService.updateRole(9L, request, admin()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户管理页不允许创建或切换管理员角色");
    }

    @Test
    void allowsEditingAdminProfileWithoutChangingAdminRole() {
        User existing = admin();
        when(userMapper.selectById(1L)).thenReturn(existing);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRealName("管理员");
        request.setRole("admin");

        User updated = adminUserService.update(1L, request, admin());

        assertThat(updated.getRole()).isEqualTo("admin");
        assertThat(updated.getRealName()).isEqualTo("管理员");
        verify(userMapper).updateById(existing);
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setRole("admin");
        user.setStatus(1);
        return user;
    }

    private User organizer() {
        User user = new User();
        user.setId(2L);
        user.setRole("organizer");
        user.setStatus(1);
        return user;
    }

    private User student() {
        User user = new User();
        user.setId(9L);
        user.setUsername("student001");
        user.setPassword("123456");
        user.setRealName("学生");
        user.setRole("student");
        user.setStatus(1);
        return user;
    }
}
