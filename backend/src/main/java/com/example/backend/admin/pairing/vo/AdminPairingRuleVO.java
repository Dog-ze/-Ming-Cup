package com.example.backend.admin.pairing.vo;

import com.example.backend.pairing.vo.PairingFoodVO;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPairingRuleVO(
        Long id, String teaCategory, List<PairingFoodVO> foods,
        String advice, LocalDateTime updatedAt
) {
}