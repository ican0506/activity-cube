package com.activitycube.util;

import com.activitycube.entity.User;

import java.util.Optional;

public final class UserContext {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static Optional<User> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
