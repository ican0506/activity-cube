package com.activitycube.util;

import java.util.Optional;

public final class TokenUtil {
    private static final String PREFIX = "mock-token-";

    private TokenUtil() {
    }

    public static String createToken(Long userId) {
        return PREFIX + userId;
    }

    public static Optional<Long> parseUserId(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(token.substring(PREFIX.length())));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
