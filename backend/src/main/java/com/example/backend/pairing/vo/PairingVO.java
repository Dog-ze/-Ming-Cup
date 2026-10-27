package com.example.backend.pairing.vo;

import java.util.List;

public record PairingVO(
        String teaCategory,
        List<PairingFoodVO> foods,
        String advice,
        String source,
        String disclaimer
) {
}
