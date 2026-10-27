package com.example.backend.security;

import com.example.backend.admin.auth.AdminAuthService;
import com.example.backend.admin.auth.AdminJwtService;
import com.example.backend.admin.auth.AdminTokenRevocationService;
import com.example.backend.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@AllArgsConstructor
public class AdminJwtInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private final AdminJwtService adminJwtService;
    private final AdminAuthService adminAuthService;
    private final AdminTokenRevocationService adminTokenRevocationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            throw new BizException(40101, "请先登录");
        }
        AdminJwtService.Principal principal = adminJwtService.parse(
                authorization.substring(BEARER_PREFIX.length()));
        adminAuthService.ensureEnabled(principal.adminId());
        if (adminTokenRevocationService.isRevoked(principal.tokenId())) {
            throw new BizException(40101, "登录已失效，请重新登录");
        }
        AdminCurrentUser.set(principal.adminId(), principal.tokenId(), principal.expiresAt());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        AdminCurrentUser.clear();
    }
}
