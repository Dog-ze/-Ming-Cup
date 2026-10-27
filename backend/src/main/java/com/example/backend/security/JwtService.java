package com.example.backend.security;

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

@Service
public class JwtService {
    public record TokenResult(String token, Instant expiresAt) {
    }
    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours}") long expireHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expireHours);
    }

    public String createToken(Long userId, String username) {
        return createTokenResult(userId, username).token();
    }

    public TokenResult createTokenResult(Long userId, String username) {
        Instant expiresAt = Instant.now().plus(expiration);
        String token = Jwts.builder().subject(String.valueOf(userId))
                .claim("userId", userId).claim("username", username).claim("tokenType", "user")
                .issuedAt(Date.from(Instant.now())).expiration(Date.from(expiresAt))
                .signWith(signingKey).compact();
        return new TokenResult(token, expiresAt);
    }

    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();
            Long userId = claims.get("userId", Long.class);
            if (!"user".equals(claims.get("tokenType", String.class)) || userId == null
                    || !String.valueOf(userId).equals(claims.getSubject())) {
                throw new BizException(40101, "登录已过期，请重新登录");
            }
            return userId;
        } catch (BizException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BizException(40101, "登录已过期，请重新登录");
        }
    }
}