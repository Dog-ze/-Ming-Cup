package com.example.backend.auth;

import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.auth.vo.AuthResponse;
import com.example.backend.common.Result;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import com.example.backend.common.BizException;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    /**
     * 用户注册接口
     * @param request
     * @return
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    /**
     * 用户登录接口
     * @param request
     * @return
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String key = httpRequest.getRemoteAddr() + ":" + request.account().trim().toLowerCase();
        loginRateLimiter.check(key);
        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.success(key);
            return Result.ok(response);
        } catch (BizException exception) {
            if (exception.getCode() == 40102) loginRateLimiter.failure(key);
            throw exception;
        }
    }
}
