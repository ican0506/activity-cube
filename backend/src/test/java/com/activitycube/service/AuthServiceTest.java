package com.activitycube.service;

import com.activitycube.common.BusinessException;
import com.activitycube.dto.LoginRequest;
import com.activitycube.dto.OrganizerCreateRequest;
import com.activitycube.dto.RegisterUserRequest;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.vo.LoginResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final PasswordService passwordService = new PasswordService();
    private final AuthService authService = new AuthService(userMapper, operationLogService, passwordService);

    @Test
    void registersStudentUserAndReturnsToken() {
        RegisterUserRequest request = validRequest();
        AtomicReference<String> storedPassword = new AtomicReference<>();
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            storedPassword.set(user.getPassword());
            user.setId(9L);
            return 1;
        });

        LoginResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock-token-9");
        assertThat(response.getUser().getUsername()).isEqualTo("2321241389");
        assertThat(response.getUser().getStudentNo()).isEqualTo("2321241389");
        assertThat(response.getUser().getGradeYear()).isEqualTo("2023级");
        assertThat(response.getUser().getMajorCode()).isEqualTo("21241");
        assertThat(response.getUser().getMajorName()).isEqualTo("软件工程");
        assertThat(response.getUser().getRole()).isEqualTo("student");
        assertThat(response.getUser().getPassword()).isNull();
        verify(userMapper).insert(any(User.class));
        assertThat(storedPassword.get()).isNotEqualTo("123456");
        assertThat(passwordService.matches("123456", storedPassword.get())).isTrue();
    }

    @Test
    void usesStudentNumberAsAccountWhenPublicRegistrationDoesNotProvideUsername() {
        RegisterUserRequest request = validRequest();
        request.setUsername(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(10L);
            return 1;
        });

        LoginResponse response = authService.register(request);

        assertThat(response.getUser().getUsername()).isEqualTo("2321241389");
        assertThat(response.getUser().getRole()).isEqualTo("student");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在");
    }

    @Test
    void rejectsInvalidStudentNumberDuringRegistration() {
        RegisterUserRequest request = validRequest();
        request.setStudentNo("T2024001");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学号必须为10位数字");
    }

    @Test
    void ignoresSubmittedRoleDuringPublicRegistration() {
        RegisterUserRequest request = validRequest();
        request.setRole("admin");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            user.setId(11L);
            return 1;
        });

        LoginResponse response = authService.register(request);

        assertThat(response.getUser().getRole()).isEqualTo("student");
    }

    @Test
    void loginIdentifierCanMatchUsernameStudentNoOrWorkNo() {
        User organizer = new User();
        organizer.setId(18L);
        organizer.setUsername("T2024001");
        organizer.setWorkNo("T2024001");
        organizer.setPassword("123456");
        organizer.setRole("organizer");
        organizer.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(organizer);

        authService.login(loginRequest("T2024001"));

        ArgumentCaptor<QueryWrapper<User>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(userMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("work_no");
    }

    @Test
    void loginAcceptsBcryptPassword() {
        User organizer = new User();
        organizer.setId(18L);
        organizer.setUsername("T2024001");
        organizer.setPassword(passwordService.encode("123456"));
        organizer.setRole("organizer");
        organizer.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(organizer);

        LoginResponse response = authService.login(loginRequest("T2024001"));

        assertThat(response.getToken()).isEqualTo("mock-token-18");
        assertThat(response.getUser().getPassword()).isNull();
    }

    @Test
    void loginRequestCanUseWorkNoFieldAsIdentifier() {
        User organizer = new User();
        organizer.setId(19L);
        organizer.setUsername("T2024001");
        organizer.setWorkNo("T2024001");
        organizer.setPassword("123456");
        organizer.setRole("organizer");
        organizer.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(organizer);

        LoginRequest request = new LoginRequest();
        request.setWorkNo("T2024001");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertThat(response.getUser().getRole()).isEqualTo("organizer");
    }

    @Test
    void adminCreatesOrganizerAccount() {
        AtomicReference<String> storedPassword = new AtomicReference<>();
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            storedPassword.set(user.getPassword());
            user.setId(18L);
            return 1;
        });

        User created = authService.createOrganizer(organizerRequest(), admin());

        assertThat(created.getId()).isEqualTo(18L);
        assertThat(created.getUsername()).isEqualTo("T2024001");
        assertThat(created.getRole()).isEqualTo("organizer");
        assertThat(created.getWorkNo()).isEqualTo("T2024001");
        assertThat(created.getStudentNo()).isNull();
        assertThat(created.getCollege()).isEqualTo("校团委");
        assertThat(created.getPassword()).isNull();
        verify(userMapper).insert(any(User.class));
        assertThat(storedPassword.get()).isNotEqualTo("123456");
        assertThat(passwordService.matches("123456", storedPassword.get())).isTrue();
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
        request.setStudentNo("2321241389");
        request.setCampus("龙子湖校区");
        request.setCollege("信息工程学院");
        request.setMajorName("软件工程");
        request.setClassName("软件工程2301");
        request.setPhone("13800009999");
        return request;
    }

    private OrganizerCreateRequest organizerRequest() {
        OrganizerCreateRequest request = new OrganizerCreateRequest();
        request.setPassword("123456");
        request.setRealName("李老师");
        request.setWorkNo("T2024001");
        request.setPhone("13800001111");
        request.setCampus("龙子湖校区");
        request.setCollege("校团委");
        return request;
    }

    private LoginRequest loginRequest(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("123456");
        return request;
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setRole("admin");
        return user;
    }
}
