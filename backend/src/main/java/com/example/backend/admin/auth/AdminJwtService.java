package com.example.backend.admin.auth;

import com.example.backend.common.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AdminJwtService {
    public record TokenResult(String token, Instant expiresAt) {
    }

    public record Principal(Long adminId, String username, String tokenId, Instant expiresAt) {
    }

    private final SecretKey signingKey;
    private final Duration expiration;

    public AdminJwtService(@Value("${app.admin-jwt.secret}") String secret,
                           @Value("${app.admin-jwt.expire-hours}") long expireHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expireHours);
    }

    public TokenResult createToken(Long adminId, String username) {
        Instant expiresAt = Instant.now().plus(expiration);
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(adminId))
                .claim("adminId", adminId)
                .claim("username", username)
                .claim("tokenType", "admin")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new TokenResult(token, expiresAt);
    }

    public Principal parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();
            if (!"admin".equals(claims.get("tokenType", String.class))) {
                throw new BizException(40101, "登录已失效，请重新登录");
            }
            Long adminId = claims.get("adminId", Long.class);
            String tokenId = claims.getId();
            if (adminId == null || tokenId == null || tokenId.isBlank()
                    || !String.valueOf(adminId).equals(claims.getSubject())) {
                throw new BizException(40101, "登录已失效，请重新登录");
            }
            return new Principal(adminId, claims.get("username", String.class), tokenId,
                    claims.getExpiration().toInstant());
        } catch (BizException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BizException(40101, "登录已失效，请重新登录");
        }
    }
}
