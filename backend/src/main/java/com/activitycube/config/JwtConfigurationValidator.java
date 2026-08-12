package com.activitycube.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtConfigurationValidator {
    private final JwtProperties properties;
    private final Environment environment;

    @PostConstruct
    public void validateProductionSecret() {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        if (productionProfile && !StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("生产环境必须配置 JWT_SECRET");
        }
    }
}
