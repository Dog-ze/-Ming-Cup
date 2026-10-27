package com.example.backend.security;

import com.example.backend.admin.auth.AdminAuthService;
import com.example.backend.admin.auth.AdminJwtService;
import com.example.backend.admin.auth.AdminTokenRevocationService;
import com.example.backend.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminJwtInterceptorTest {

    @AfterEach
    void clearCurrentAdmin() {
        AdminCurrentUser.clear();
    }

    @Test
    void rejectsATokenThatWasRevokedByLogout() {
        AdminJwtService jwtService = mock(AdminJwtService.class);
        AdminAuthService authService = mock(AdminAuthService.class);
        AdminTokenRevocationService revocationService = mock(AdminTokenRevocationService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        when(jwtService.parse("old-token")).thenReturn(
                new AdminJwtService.Principal(1L, "admin", "jti-old", Instant.now().plusSeconds(3600)));
        when(revocationService.isRevoked("jti-old")).thenReturn(true);

        AdminJwtInterceptor interceptor = new AdminJwtInterceptor(jwtService, authService, revocationService);

        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), mock(HandlerMethod.class)))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode()).isEqualTo(40101));
    }
}
