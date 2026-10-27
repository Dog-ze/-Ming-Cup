package com.example.backend.security;

import com.example.backend.common.BizException;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtInterceptor(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    private boolean isOptionalLoginPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/recommend".equals(path) || "/api/pairing/stores/nearby".equals(path);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            if (isOptionalLoginPath(request)) return true;
            throw new BizException(40101, "请先登录");
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            throw new BizException(40101, "登录凭证格式错误");
        }
        Long userId = jwtService.parseUserId(authorization.substring(BEARER_PREFIX.length()));
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(40101, "登录已失效，请重新登录");
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new BizException(40103, "账号已停用");
        CurrentUser.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        CurrentUser.clear();
    }
}