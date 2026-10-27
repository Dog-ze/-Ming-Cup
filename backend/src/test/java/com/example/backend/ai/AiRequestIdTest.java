package com.example.backend.ai;

import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.ai.dto.BrewGuideRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiRequestIdTest {

    @Test
    void keepsTheClientOperationIdForChatAndBrewGuide() {
        AiChatRequest chat = new AiChatRequest("下午容易犯困", "泉州市", null, null, "chat-001");
        BrewGuideRequest brewGuide = new BrewGuideRequest(12L, "泉州市", null, null, "brew-001");

        assertThat(chat.requestId()).isEqualTo("chat-001");
        assertThat(brewGuide.requestId()).isEqualTo("brew-001");
    }
}
