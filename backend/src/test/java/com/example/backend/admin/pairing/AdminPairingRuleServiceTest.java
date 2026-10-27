package com.example.backend.admin.pairing;

import com.example.backend.admin.pairing.dto.PairingFoodSaveRequest;
import com.example.backend.admin.pairing.dto.PairingRuleSaveRequest;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.entity.PairingRule;
import com.example.backend.mapper.PairingRuleMapper;
import com.example.backend.security.AdminCurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminPairingRuleServiceTest {

    @AfterEach
    void clearCurrentAdmin() {
        AdminCurrentUser.clear();
    }

    @Test
    void createStoresEachFoodNameWithItsImageUrl() {
        PairingRuleMapper pairingRuleMapper = mock(PairingRuleMapper.class);
        AdminOperationLogService operationLogService = mock(AdminOperationLogService.class);
        AdminPairingRuleService service = new AdminPairingRuleService(
                pairingRuleMapper, operationLogService, new ObjectMapper());
        AdminCurrentUser.set(1L, Instant.now().plusSeconds(3600));

        service.create(new PairingRuleSaveRequest("绿茶", List.of(
                new PairingFoodSaveRequest("水果", "/uploads/admin/pairing-food/fruit.jpg")
        ), "适合搭配清淡食物。"));

        ArgumentCaptor<PairingRule> captor = ArgumentCaptor.forClass(PairingRule.class);
        verify(pairingRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getFoods())
                .isEqualTo("[{\"name\":\"水果\",\"imageUrl\":\"/uploads/admin/pairing-food/fruit.jpg\"}]");
    }
}
