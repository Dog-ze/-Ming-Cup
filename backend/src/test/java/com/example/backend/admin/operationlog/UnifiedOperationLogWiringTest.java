package com.example.backend.admin.operationlog;

import com.example.backend.admin.auth.AdminAuthService;
import com.example.backend.admin.auth.AdminJwtService;
import com.example.backend.admin.pairing.AdminPairingRuleService;
import com.example.backend.admin.pairing.dto.PairingFoodSaveRequest;
import com.example.backend.admin.pairing.dto.PairingRuleSaveRequest;
import com.example.backend.admin.store.AdminStoreService;
import com.example.backend.admin.store.dto.AdminStoreSaveRequest;
import com.example.backend.admin.tea.AdminTeaService;
import com.example.backend.admin.tea.dto.AdminTeaSaveRequest;
import com.example.backend.admin.tea.mapper.AdminTeaMapper;
import com.example.backend.admin.user.AdminUserService;
import com.example.backend.entity.PairingStore;
import com.example.backend.entity.Tea;
import com.example.backend.entity.User;
import com.example.backend.mapper.AdminUserMapper;
import com.example.backend.admin.auth.AdminTokenRevocationService;
import com.example.backend.mapper.PairingRuleMapper;
import com.example.backend.mapper.PairingStoreMapper;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.mapper.TeaRecordMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.AdminCurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 防止后台模块绕过统一操作日志服务，重新各自直接写 admin_operation_log。
 */
@ExtendWith(MockitoExtension.class)
class UnifiedOperationLogWiringTest {
    @Mock
    private AdminOperationLogService operationLogService;
    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdminJwtService adminJwtService;
    @Mock
    private AdminTokenRevocationService adminTokenRevocationService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private QuestionnaireMapper questionnaireMapper;
    @Mock
    private TeaRecordMapper teaRecordMapper;
    @Mock
    private AdminTeaMapper adminTeaMapper;
    @Mock
    private TeaMapper teaMapper;
    @Mock
    private PairingRuleMapper pairingRuleMapper;
    @Mock
    private PairingStoreMapper pairingStoreMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminAuthService authService;
    @InjectMocks
    private AdminUserService userService;
    @InjectMocks
    private AdminTeaService teaService;
    @InjectMocks
    private AdminPairingRuleService pairingRuleService;
    @InjectMocks
    private AdminStoreService storeService;

    @AfterEach
    void clearCurrentAdmin() {
        AdminCurrentUser.clear();
    }

    @Test
    void logoutUsesTheUnifiedOperationLogService() {
        authService.logout(7L, "token-7", Instant.now().plusSeconds(3600));

        verify(operationLogService).record(7L, "auth", "logout", 7L, Map.of());
        verify(adminTokenRevocationService).revoke(org.mockito.ArgumentMatchers.eq("token-7"), any());
    }

    @Test
    void userStatusChangeUsesTheUnifiedOperationLogService() {
        User user = new User();
        user.setId(21L);
        user.setStatus(1);
        when(userMapper.selectById(21L)).thenReturn(user);
        AdminCurrentUser.set(7L, Instant.now().plusSeconds(3600));

        userService.updateStatus(21L, 0);

        verify(operationLogService).recordCurrentAdmin(
                "user", "disable", 21L, Map.of("beforeStatus", 1, "afterStatus", 0)
        );
    }

    @Test
    void teaCreationUsesTheUnifiedOperationLogService() {
        doAnswer(invocation -> {
            Tea tea = invocation.getArgument(0);
            tea.setId(31L);
            return 1;
        }).when(teaMapper).insert(any(Tea.class));
        AdminCurrentUser.set(7L, Instant.now().plusSeconds(3600));

        teaService.create(teaRequest());

        verify(operationLogService).recordCurrentAdmin(
                eq("tea"), eq("create"), eq(31L),
                ArgumentMatchers.argThat((Map<String, ?> detail) ->
                        "西湖龙井".equals(detail.get("name"))
                                && "绿茶".equals(detail.get("category"))
                                && Integer.valueOf(1).equals(detail.get("enabled"))
                )
        );
    }

    @Test
    void pairingRuleCreationUsesTheUnifiedOperationLogService() {
        doAnswer(invocation -> {
            com.example.backend.entity.PairingRule rule = invocation.getArgument(0);
            rule.setId(41L);
            return 1;
        }).when(pairingRuleMapper).insert(any(com.example.backend.entity.PairingRule.class));
        AdminCurrentUser.set(7L, Instant.now().plusSeconds(3600));

        pairingRuleService.create(new PairingRuleSaveRequest(
                "绿茶", List.of(new PairingFoodSaveRequest("水果", null)), "适合搭配清淡食物。"
        ));

        verify(operationLogService).recordCurrentAdmin(
                "pairing_rule", "create", 41L, Map.of("teaCategory", "绿茶", "foodCount", 1)
        );
    }

    @Test
    void storeCreationUsesTheUnifiedOperationLogService() {
        doAnswer(invocation -> {
            PairingStore store = invocation.getArgument(0);
            store.setId(51L);
            return 1;
        }).when(pairingStoreMapper).insert(any(PairingStore.class));
        AdminCurrentUser.set(7L, Instant.now().plusSeconds(3600));

        storeService.create(new AdminStoreSaveRequest(
                "茶香小馆", "", "莆田市城厢区示例路 88 号",
                new BigDecimal("119.0077"), new BigDecimal("25.4541"), "",
                List.of("绿茶", "白茶"), List.of("水果", "清淡糕点"), "提供清淡茶点与安静品茶空间。"
        ));

        verify(operationLogService).recordCurrentAdmin(
                "pairing_store", "create", 51L,
                Map.of("storeName", "茶香小馆", "teaCategoryCount", 2, "foodTagCount", 2)
        );
    }

    private AdminTeaSaveRequest teaRequest() {
        return new AdminTeaSaveRequest(
                "西湖龙井", "绿茶", "凉", "中", "[\"清淡鲜爽\"]", "[\"提神\"]",
                "适中", 85, 90, "local", null, 1
        );
    }
}
