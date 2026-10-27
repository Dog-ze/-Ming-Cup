package com.example.backend.pairing;

import com.example.backend.entity.PairingRule;
import com.example.backend.mapper.PairingRuleMapper;
import com.example.backend.pairing.vo.PairingFoodVO;
import com.example.backend.pairing.vo.PairingVO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PairingServiceTest {

    @Test
    void returnsFoodObjectsWithTheirUploadedImages() {
        PairingRuleMapper pairingRuleMapper = mock(PairingRuleMapper.class);
        PairingRule rule = new PairingRule();
        rule.setTeaCategory("绿茶");
        rule.setFoods("[{\"name\":\"水果\",\"imageUrl\":\"/uploads/admin/pairing-food/fruit.jpg\"}]");
        rule.setAdvice("绿茶适合搭配清淡小食。");
        when(pairingRuleMapper.selectOne(any())).thenReturn(rule);

        PairingService pairingService = new PairingService(pairingRuleMapper, new ObjectMapper());

        PairingVO result = pairingService.getPairing("绿茶");

        assertThat(result.source()).isEqualTo("local_rule");
        assertThat(result.foods()).containsExactly(
                new PairingFoodVO("水果", "/uploads/admin/pairing-food/fruit.jpg"));
    }

    @Test
    void keepsOldStringArrayRulesReadableWithoutImages() {
        PairingRuleMapper pairingRuleMapper = mock(PairingRuleMapper.class);
        PairingRule rule = new PairingRule();
        rule.setTeaCategory("绿茶");
        rule.setFoods("[\"水果\",\"清淡糕点\"]");
        rule.setAdvice("绿茶适合搭配清淡小食。");
        when(pairingRuleMapper.selectOne(any())).thenReturn(rule);

        PairingService pairingService = new PairingService(pairingRuleMapper, new ObjectMapper());

        PairingVO result = pairingService.getPairing("绿茶");

        assertThat(result.foods()).containsExactly(
                new PairingFoodVO("水果", null),
                new PairingFoodVO("清淡糕点", null));
    }
}
