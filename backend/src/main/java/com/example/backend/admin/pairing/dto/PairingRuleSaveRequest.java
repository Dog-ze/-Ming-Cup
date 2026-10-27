package com.example.backend.admin.pairing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PairingRuleSaveRequest(
        @NotBlank(message = "茶类不能为空") @Size(max = 16, message = "茶类不能超过16个字符") String teaCategory,
        @NotEmpty(message = "请至少填写一种推荐食品") @Size(max = 10, message = "推荐食品不能超过10项")
        List<@Valid PairingFoodSaveRequest> foods,
        @NotBlank(message = "搭配建议不能为空") @Size(max = 300, message = "搭配建议不能超过300个字符") String advice
) {
}