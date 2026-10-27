package com.example.backend.admin.auth;

import com.example.backend.admin.auth.dto.AdminLoginRequest;
import com.example.backend.admin.auth.vo.AdminLoginVO;
import com.example.backend.admin.auth.vo.AdminProfileVO;
import com.example.backend.common.Result;
import com.example.backend.security.AdminCurrentUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@AllArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录。
     * @param request 登录请求
     * @return 登录结果，包括管理员信息和 JWT token
     */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    /**
     * 获取当前登录管理员的个人信息。
     * @return 当前登录管理员的个人信息
     */
    @GetMapping("/me")
    public Result<AdminProfileVO> me() {
        return Result.ok(adminAuthService.me(AdminCurrentUser.id(), AdminCurrentUser.expiresAt()));
    }

    /**
     * 管理员登出。
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        adminAuthService.logout(AdminCurrentUser.id(), AdminCurrentUser.tokenId(), AdminCurrentUser.expiresAt());
        return Result.ok(true);
    }
}
