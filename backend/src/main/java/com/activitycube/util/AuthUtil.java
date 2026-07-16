package com.activitycube.util;

import com.activitycube.common.BusinessException;
import com.activitycube.entity.User;

public final class AuthUtil {
    private AuthUtil() {
    }

    public static User requireUser() {
        return UserContext.get().orElseThrow(() -> new BusinessException("请先登录"));
    }

    public static void requireOrganizerOrAdmin(User user) {
        if (!"organizer".equals(user.getRole()) && !"admin".equals(user.getRole())) {
            throw new BusinessException("无活动负责人权限");
        }
    }

    public static void requireAdmin(User user) {
        if (user == null || !"admin".equals(user.getRole())) {
            throw new BusinessException("只有管理员可以创建活动负责人账号");
        }
    }
}
