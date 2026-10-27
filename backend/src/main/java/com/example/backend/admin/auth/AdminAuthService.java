package com.example.backend.admin.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.admin.auth.dto.AdminLoginRequest;
import com.example.backend.admin.auth.vo.AdminLoginVO;
import com.example.backend.admin.auth.vo.AdminProfileVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.AdminUser;
import com.example.backend.mapper.AdminUserMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@AllArgsConstructor
public class AdminAuthService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService adminJwtService;
    private final AdminTokenRevocationService adminTokenRevocationService;
    private final AdminOperationLogService operationLogService;


    public AdminLoginVO login(AdminLoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.username().trim()));
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BizException(40102, "账号或密码错误");
        }
        ensureEnabled(admin);
        AdminJwtService.TokenResult token = adminJwtService.createToken(admin.getId(), admin.getUsername());
        operationLogService.record(admin.getId(), "auth", "login", admin.getId(), Map.of());
        return new AdminLoginVO(admin.getId(), admin.getUsername(), admin.getDisplayName(), token.token(),
                toLocalDateTime(token.expiresAt()));
    }

    public AdminProfileVO me(Long adminId, Instant expiresAt) {
        AdminUser admin = getById(adminId);
        ensureEnabled(admin);
        return new AdminProfileVO(admin.getId(), admin.getUsername(), admin.getDisplayName(), toLocalDateTime(expiresAt));
    }

    public void logout(Long adminId, String tokenId, Instant expiresAt) {
        adminTokenRevocationService.revoke(tokenId, expiresAt);
        operationLogService.record(adminId, "auth", "logout", adminId, Map.of());
    }

    public void ensureEnabled(Long adminId) { ensureEnabled(getById(adminId)); }

    private AdminUser getById(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) throw new BizException(40101, "登录已失效，请重新登录");
        return admin;
    }

    private void ensureEnabled(AdminUser admin) {
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BizException(40103, "管理员账号已停用");
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, BUSINESS_ZONE);
    }
}
