package com.activitycube.config.security;

import com.activitycube.config.JwtProperties;
import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        TokenUtil.class,
        SecurityConfigTest.TestEndpoints.class
})
class SecurityConfigTest {
    private static final String SECRET = "12345678901234567890123456789012";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUpJwtProperties() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getExpirationSeconds()).thenReturn(7200L);
        when(jwtProperties.getIssuer()).thenReturn("activity-cube");
    }

    @Test
    void loginIsAllowedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("login"));
    }

    @Test
    void registerIsAllowedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/register"))
                .andExpect(status().isOk())
                .andExpect(content().string("register"));
    }

    @Test
    void apiRequiresToken() throws Exception {
        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void apiAcceptsValidJwtAndUsesDatabaseRole() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "student", 1));

        mockMvc.perform(get("/api/activities").header(HttpHeaders.AUTHORIZATION, bearer(9L, "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("student"));
    }

    @Test
    void tamperedJwtIsRejected() throws Exception {
        String token = tokenUtil.createToken(9L, "student");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get("/api/activities").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        String expiredToken = new TokenUtil(jwtProperties(-1)).createToken(9L, "student");

        mockMvc.perform(get("/api/activities").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void missingDatabaseUserIsRejected() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(null);

        mockMvc.perform(get("/api/activities").header(HttpHeaders.AUTHORIZATION, bearer(9L, "student")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void disabledUserIsRejected() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "student", 0));

        mockMvc.perform(get("/api/activities").header(HttpHeaders.AUTHORIZATION, bearer(9L, "student")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void studentCannotAccessAdminOnlyApi() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "student", 1));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(9L, "student")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你没有权限访问该功能"));
    }

    @Test
    void organizerCanPassGeneralAdminApiSecurityLayer() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "organizer", 1));

        mockMvc.perform(get("/api/admin/notices").header(HttpHeaders.AUTHORIZATION, bearer(9L, "organizer")))
                .andExpect(status().isOk())
                .andExpect(content().string("organizer"));
    }

    @Test
    void organizerCannotAccessAdminOnlyApi() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "organizer", 1));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(9L, "organizer")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你没有权限访问该功能"));
    }

    @Test
    void adminCanAccessAdminOnlyApi() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "admin", 1));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(9L, "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void optionsRequestsAreNotBlocked() throws Exception {
        mockMvc.perform(options("/api/activities")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isOk());
    }

    @Test
    void uploadsArePublic() throws Exception {
        mockMvc.perform(get("/uploads/demo.png"))
                .andExpect(status().isOk())
                .andExpect(content().string("upload"));
    }

    private String bearer(Long userId, String tokenRole) {
        return "Bearer " + tokenUtil.createToken(userId, tokenRole);
    }

    private User user(Long id, String role, Integer status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static JwtProperties jwtProperties(long expirationSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationSeconds(expirationSeconds);
        properties.setIssuer("activity-cube");
        return properties;
    }

    @RestController
    static class TestEndpoints {
        @PostMapping("/api/auth/login")
        String login() {
            return "login";
        }

        @PostMapping("/api/auth/register")
        String register() {
            return "register";
        }

        @GetMapping("/api/activities")
        String activities(Authentication authentication) {
            assertThat(authentication).isNotNull();
            User user = (User) authentication.getPrincipal();
            return user.getRole();
        }

        @GetMapping("/api/admin/users")
        String adminUsers(Authentication authentication) {
            User user = (User) authentication.getPrincipal();
            return user.getRole();
        }

        @GetMapping("/api/admin/notices")
        String adminNotices(Authentication authentication) {
            User user = (User) authentication.getPrincipal();
            return user.getRole();
        }

        @GetMapping("/uploads/demo.png")
        String upload() {
            return "upload";
        }
    }
}
