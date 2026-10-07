package com.example.microservice.security;

import com.example.microservice.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new ForbiddenException("未登录或登录已过期");
        }
        return user;
    }

    public static Long getCurrentUserId() {
        return currentUser().id();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentUser().role());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("无权限访问");
        }
    }

    public static void requireOwnerOrAdmin(Long userId) {
        CurrentUser user = currentUser();
        if (!"ADMIN".equals(user.role()) && !user.id().equals(userId)) {
            throw new ForbiddenException("无权访问该资源");
        }
    }
}
