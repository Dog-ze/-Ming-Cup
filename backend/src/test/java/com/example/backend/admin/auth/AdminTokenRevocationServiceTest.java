package com.example.backend.admin.auth;

import com.example.backend.mapper.AdminTokenRevocationMapper;
import com.example.backend.entity.AdminTokenRevocation;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTokenRevocationServiceTest {

    @Test
    void revokedTokenIsRecordedAndThenRejected() {
        AdminTokenRevocationMapper mapper = mock(AdminTokenRevocationMapper.class);
        AdminTokenRevocationService service = new AdminTokenRevocationService(mapper);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        service.revoke("token-1", expiresAt);
        when(mapper.selectCount(any())).thenReturn(1L);

        assertThat(service.isRevoked("token-1")).isTrue();
        verify(mapper).insertIgnore(any(AdminTokenRevocation.class));
    }

    @Test
    void repeatedLogoutForTheSameTokenIsIdempotent() {
        AdminTokenRevocationMapper mapper = mock(AdminTokenRevocationMapper.class);
        when(mapper.insertIgnore(any(AdminTokenRevocation.class))).thenReturn(0);
        AdminTokenRevocationService service = new AdminTokenRevocationService(mapper);

        service.revoke("token-1", Instant.now().plusSeconds(3600));

        verify(mapper).insertIgnore(any(AdminTokenRevocation.class));
        verify(mapper, never()).insert(any(AdminTokenRevocation.class));
    }

    @Test
    void expiredRevocationsAreCleanedUp() {
        AdminTokenRevocationMapper mapper = mock(AdminTokenRevocationMapper.class);
        AdminTokenRevocationService service = new AdminTokenRevocationService(mapper);

        service.removeExpired();

        verify(mapper).delete(any());
    }
}
