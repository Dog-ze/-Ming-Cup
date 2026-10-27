package com.example.backend.user;

import com.example.backend.user.dto.ProfileUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsCurrentLocationAsTheCitySavedToUserProfile() {
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                null,
                "当前位置",
                new BigDecimal("119.0077"),
                new BigDecimal("25.4541")
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("城市必须填写详细地址");
    }
}
