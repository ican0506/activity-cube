package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.OrganizerCreateRequest;
import com.activitycube.dto.RegisterUserRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.vo.LoginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final AuthService authService = new AuthService(userMapper);

    @Test
    void registersStudentUserAndReturnsToken() {
        RegisterUserRequest request = validRequest();
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(9L);
            return 1;
        });

        LoginResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock-token-9");
        assertThat(response.getUser().getUsername()).isEqualTo("newstudent");
        assertThat(response.getUser().getRole()).isEqualTo("student");
        assertThat(response.getUser().getPassword()).isNull();
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在");
    }

    @Test
    void adminCreatesOrganizerAccount() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(18L);
            return 1;
        });

        User created = authService.createOrganizer(organizerRequest(), admin());

        assertThat(created.getId()).isEqualTo(18L);
        assertThat(created.getUsername()).isEqualTo("org001");
        assertThat(created.getRole()).isEqualTo("organizer");
        assertThat(created.getStudentNo()).isEqualTo("T2024001");
        assertThat(created.getCollege()).isEqualTo("校团委");
        assertThat(created.getPassword()).isNull();
    }

    @Test
    void rejectsOrganizerCreationByNonAdmin() {
        User operator = new User();
        operator.setRole("organizer");

        assertThatThrownBy(() -> authService.createOrganizer(organizerRequest(), operator))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有管理员可以创建活动负责人账号");
    }

    @Test
    void rejectsDuplicateOrganizerUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.createOrganizer(organizerRequest(), admin()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在");
    }

    private RegisterUserRequest validRequest() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("newstudent");
        request.setPassword("123456");
        request.setRealName("新同学");
        request.setStudentNo("2024999");
        request.setCampus("龙子湖校区");
        request.setCollege("信息工程学院");
        request.setMajorClass("软件工程2401");
        request.setPhone("13800009999");
        return request;
    }

    private OrganizerCreateRequest organizerRequest() {
        OrganizerCreateRequest request = new OrganizerCreateRequest();
        request.setUsername("org001");
        request.setPassword("123456");
        request.setRealName("活动负责人");
        request.setStudentNo("T2024001");
        request.setPhone("13800001111");
        request.setCampus("龙子湖校区");
        request.setCollege("校团委");
        return request;
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setRole("admin");
        return user;
    }
}
