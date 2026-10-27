package com.example.backend.admin.store.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record StorePromotionRequest(
        @NotNull @Min(0) @Max(1) Integer promotionEnabled,
        @NotNull @Min(0) @Max(30) Integer promotionScore,
        LocalDateTime promotionStartAt,
        LocalDateTime promotionEndAt
) { }