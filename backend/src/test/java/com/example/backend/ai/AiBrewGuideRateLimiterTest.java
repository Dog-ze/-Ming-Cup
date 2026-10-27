package com.example.backend.ai;

import com.example.backend.common.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBrewGuideRateLimiterTest {

    @Test
    void limitsEachSceneAndUserIndependentlyWithinOneMinute() {
        AiProperties properties = new AiProperties();
        properties.setBrewGuideRequestsPerMinute(2);
        properties.setChatRequestsPerMinute(2);
        AiBrewGuideRateLimiter limiter = new AiBrewGuideRateLimiter(properties);

        limiter.check(8L, "brew-guide");
        limiter.check(8L, "brew-guide");

        assertThatThrownBy(() -> limiter.check(8L, "brew-guide"))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(42901));
        assertThatCode(() -> limiter.check(8L, "chat")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.check(9L, "brew-guide")).doesNotThrowAnyException();
    }
}
