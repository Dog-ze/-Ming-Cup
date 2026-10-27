package com.example.backend.admin.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.AdminTokenRevocation;
import com.example.backend.mapper.AdminTokenRevocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AdminTokenRevocationService {
    private final AdminTokenRevocationMapper adminTokenRevocationMapper;

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || !expiresAt.isAfter(Instant.now())) return;

        AdminTokenRevocation revocation = new AdminTokenRevocation();
        revocation.setTokenId(tokenId);
        revocation.setExpiresAt(toUtc(expiresAt));
        revocation.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        adminTokenRevocationMapper.insertIgnore(revocation);
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) return true;
        return adminTokenRevocationMapper.selectCount(new LambdaQueryWrapper<AdminTokenRevocation>()
                .eq(AdminTokenRevocation::getTokenId, tokenId)
                .gt(AdminTokenRevocation::getExpiresAt, LocalDateTime.now(ZoneOffset.UTC))) > 0;
    }

    /** 已过期的 JWT 不再需要保留撤销记录。 */
    @Scheduled(cron = "0 15 3 * * *")
    public void removeExpired() {
        adminTokenRevocationMapper.delete(new LambdaQueryWrapper<AdminTokenRevocation>()
                .lt(AdminTokenRevocation::getExpiresAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
