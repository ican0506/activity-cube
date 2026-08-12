package com.activitycube.config;

import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.TokenUtil;
import com.activitycube.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {
    private static final String SECRET = "12345678901234567890123456789012";

    private final UserMapper userMapper = mock(UserMapper.class);
    private final TokenUtil tokenUtil = new TokenUtil(jwtProperties("activity-cube"));
    private final AuthInterceptor interceptor = new AuthInterceptor(userMapper, tokenUtil);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request("/api/activities"), response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(UserContext.get()).isEmpty();
    }

    @Test
    void acceptsValidTokenAndSetsUserContext() throws Exception {
        User user = user(9L, "student", 1);
        when(userMapper.selectById(9L)).thenReturn(user);
        MockHttpServletRequest request = request("/api/activities");
        request.addHeader("Authorization", "Bearer " + tokenUtil.createToken(9L, "student"));

        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(passed).isTrue();
        assertThat(UserContext.get()).contains(user);
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = tokenUtil.createToken(9L, "student");
        MockHttpServletRequest request = request("/api/activities");
        request.addHeader("Authorization", "Bearer " + token.substring(0, token.length() - 2) + "xx");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(UserContext.get()).isEmpty();
    }

    @Test
    void rejectsDisabledUserEvenWhenTokenIsValid() throws Exception {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "student", 0));
        MockHttpServletRequest request = request("/api/activities");
        request.addHeader("Authorization", "Bearer " + tokenUtil.createToken(9L, "student"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request, response, new Object());

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(UserContext.get()).isEmpty();
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    private User user(Long id, String role, Integer status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private JwtProperties jwtProperties(String issuer) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationSeconds(7200);
        properties.setIssuer(issuer);
        return properties;
    }
}
