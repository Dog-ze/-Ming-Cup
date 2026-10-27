package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.AdminTokenRevocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface AdminTokenRevocationMapper extends BaseMapper<AdminTokenRevocation> {
    /** 同一 Token 的重复退出只保留一条撤销记录。 */
    @Insert("""
            INSERT IGNORE INTO admin_token_revocation (token_id, expires_at, revoked_at)
            VALUES (#{tokenId}, #{expiresAt}, #{revokedAt})
            """)
    int insertIgnore(AdminTokenRevocation revocation);
}
