package com.activitycube.config;

import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.TokenUtil;
import com.activitycube.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(request.getRequestURI())
                || "/api/auth/register".equals(request.getRequestURI())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        User user = TokenUtil.parseUserId(token)
                .map(userMapper::selectById)
                .orElse(null);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号已禁用，请联系管理员\",\"data\":null}");
            return false;
        }
        if (!hasAdminApiPermission(request.getRequestURI(), user)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"你没有权限访问该功能\",\"data\":null}");
            return false;
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean hasAdminApiPermission(String uri, User user) {
        if (isAdminOnlyPath(uri)) {
            return "admin".equals(user.getRole());
        }
        if (uri.startsWith("/api/admin/")) {
            return "admin".equals(user.getRole()) || "organizer".equals(user.getRole());
        }
        return true;
    }

    private boolean isAdminOnlyPath(String uri) {
        return uri.startsWith("/api/admin/users")
                || uri.startsWith("/api/admin/notices/system")
                || uri.startsWith("/api/admin/operation-logs")
                || uri.startsWith("/api/activities/admin/");
    }
}
