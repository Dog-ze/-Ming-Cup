package com.example.backend.auth;

import com.example.backend.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsChineseUsernameWithinTenCharacters() {
        RegisterRequest request = new RegisterRequest(
                "一盏茗用户", "13800138000", "tea@example.com", "123456"
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
