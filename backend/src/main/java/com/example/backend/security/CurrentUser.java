package com.example.backend.security;

/**
 * 保存当前 HTTP 请求中的登录用户 ID。
 */
public final class CurrentUser {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long id() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求未登录");
        }
        return userId;
    }

    // 新增：游客访问时返回 null，不报错
    public static Long idOrNull() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
