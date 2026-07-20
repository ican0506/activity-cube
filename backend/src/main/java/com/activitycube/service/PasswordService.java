package com.activitycube.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordService {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    public boolean isBcrypt(String storedPassword) {
        return StringUtils.hasText(storedPassword)
                && (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }
}
