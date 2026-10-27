package com.example.backend.auth;

import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.vo.AuthResponse;
import com.example.backend.common.BizException;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void allowsSameUsernameWhenPhoneAndEmailAreDifferent() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(jwtService.createTokenResult(anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new JwtService.TokenResult("test-token", Instant.parse("2026-10-02T00:00:00Z")));
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(user.getPhone().equals("13800138000") ? 1L : 2L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        AuthService authService = new AuthService(userMapper, passwordEncoder, jwtService);

        AuthResponse first = authService.register(
                new RegisterRequest("tea_user", "13800138000", "first@example.com", "123456")
        );
        AuthResponse second = authService.register(
                new RegisterRequest("tea_user", "13900139000", "second@example.com", "123456")
        );

        assertThat(first.username()).isEqualTo("tea_user");
        assertThat(second.username()).isEqualTo("tea_user");
    }

    @Test
    void rejectsDisabledUserAfterPasswordVerificationWithoutCreatingToken() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        User disabledUser = new User();
        disabledUser.setId(8L);
        disabledUser.setUsername("disabled_tea_user");
        disabledUser.setPasswordHash("encoded-password");
        disabledUser.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(disabledUser);
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);
        AuthService authService = new AuthService(userMapper, passwordEncoder, jwtService);

        assertThatThrownBy(() -> authService.login(new LoginRequest("phone", "13800138000", "123456")))
                .isInstanceOf(BizException.class)
                .hasMessage("账号已停用");

        verify(jwtService, never()).createTokenResult(anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
