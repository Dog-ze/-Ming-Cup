package com.example.backend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.auth.vo.AuthResponse;
import com.example.backend.common.BizException;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String phone = request.phone().trim();
        String email = request.email().trim().toLowerCase();

        ensureUnique(User::getPhone, phone, "手机号已被使用");
        ensureUnique(User::getEmail, email, "邮箱已被使用");

        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCity("北京市");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 防止两个相同手机号或邮箱的注册请求同时通过上方查询。
            throw new BizException(409, "手机号或邮箱已被使用");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(40103, "账号已停用");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String loginType = request.loginType().trim();
        String account = request.account().trim();
        User user = switch (loginType) {
            case "phone" -> userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, account)
            );
            case "email" -> userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, account.toLowerCase())
            );
            default -> throw new BizException(400, "登录方式只能是 phone 或 email");
        };

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(40102, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(40103, "账号已停用");
        }

        return buildAuthResponse(user);
    }

    /**
     * 确认手机号或邮箱未被其他账户使用。
     */
    private void ensureUnique(com.baomidou.mybatisplus.core.toolkit.support.SFunction<User, ?> field,
                              String value,
                              String message) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(field, value));
        if (count != null && count > 0) {
            throw new BizException(409, message);
        }
    }

    /**
     * 构建登录或注册成功的响应对象。
     * @param user
     * @return
     */
    private AuthResponse buildAuthResponse(User user) {
        JwtService.TokenResult tokenResult = jwtService.createTokenResult(user.getId(), user.getUsername());
        LocalDateTime expiresAt = LocalDateTime.ofInstant(tokenResult.expiresAt(), ZoneId.of("Asia/Shanghai"));

        return new AuthResponse(
                user.getId(), user.getUsername(), tokenResult.token(), expiresAt
        );
    }
}
