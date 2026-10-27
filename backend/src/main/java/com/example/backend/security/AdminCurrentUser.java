package com.example.backend.security;

import com.example.backend.common.BizException;
import java.time.Instant;

public final class AdminCurrentUser {
    public record Principal(Long adminId, String tokenId, Instant expiresAt) {
    }

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private AdminCurrentUser() {
    }

    public static void set(Long adminId, String tokenId, Instant expiresAt) {
        HOLDER.set(new Principal(adminId, tokenId, expiresAt));
    }

    /** 兼容只需要管理员身份的后台服务与既有测试。 */
    public static void set(Long adminId, Instant expiresAt) {
        set(adminId, "", expiresAt);
    }

    public static Long id() {
        Principal principal = HOLDER.get();
        if (principal == null) {
            throw new BizException(40101, "请先登录");
        }
        return principal.adminId();
    }

    public static Instant expiresAt() {
        Principal principal = HOLDER.get();
        if (principal == null) {
            throw new BizException(40101, "请先登录");
        }
        return principal.expiresAt();
    }

    public static String tokenId() {
        Principal principal = HOLDER.get();
        if (principal == null) {
            throw new BizException(40101, "请先登录");
        }
        return principal.tokenId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
