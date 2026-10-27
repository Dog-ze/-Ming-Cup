package com.example.backend.questionnaire.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuestionnaireSubmitRequest(
        @NotNull @Min(0) @Max(5) Integer purpose,
        @NotNull @Min(0) @Max(2) Integer constitutionType,
        @NotNull @Min(0) @Max(2) Integer insomnia,
        @NotNull @Min(0) @Max(4) Integer flavorPreference,
        @NotNull @Min(0) @Max(2) Integer strengthPreference
) {
}