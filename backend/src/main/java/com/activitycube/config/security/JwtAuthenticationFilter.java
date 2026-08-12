package com.activitycube.config.security;

import com.activitycube.entity.User;
import com.activitycube.mapper.UserMapper;
import com.activitycube.util.TokenUtil;
import com.activitycube.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenUtil tokenUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UserContext.clear();
        SecurityContextHolder.clearContext();
        try {
            String token = resolveBearerToken(request);
            if (StringUtils.hasText(token)) {
                tokenUtil.parseToken(token)
                        .map(TokenUtil.ParsedToken::userId)
                        .map(userMapper::selectById)
                        .filter(this::isEnabled)
                        .ifPresent(user -> {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(user, null, authorities(user));
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            UserContext.set(user);
                        });
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean isEnabled(User user) {
        return user != null && (user.getStatus() == null || user.getStatus() != 0);
    }

    private List<SimpleGrantedAuthority> authorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
    }
}
