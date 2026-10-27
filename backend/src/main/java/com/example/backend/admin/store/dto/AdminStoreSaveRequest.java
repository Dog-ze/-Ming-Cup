package com.example.backend.admin.store.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminStoreSaveRequest(
        @NotBlank @Size(max = 100) String storeName,
        @Size(max = 500) String coverImage,
        @NotBlank @Size(max = 300) String address,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @Size(max = 20) String phone,
        @NotEmpty @Size(max = 7) List<@NotBlank @Size(max = 16) String> teaCategories,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 30) String> foodTags,
        @NotBlank @Size(max = 500) String description
) { }