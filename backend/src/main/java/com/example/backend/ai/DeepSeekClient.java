package com.example.backend.ai;

import com.example.backend.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DeepSeekClient {
    @Qualifier("deepSeekRestClient")
    private final RestClient deepSeekRestClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public String complete(String systemPrompt, String userPrompt, int maxTokens) {
        ensureConfigured();
        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "stream", false,
                "temperature", 0.2,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            Map<?, ?> response = deepSeekRestClient
                    .post()
                    .uri(aiProperties.getBaseUrl() + "/chat/completions")
                    .headers(headers -> headers.setBearerAuth(aiProperties.getApiKey()))
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> choices = response == null ? null : (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty() || !(choices.get(0) instanceof Map<?, ?> first)) {
                throw new BizException(50301, "AI 服务未返回有效内容");
            }

            Map<?, ?> message = (Map<?, ?>) first.get("message");
            String content = message == null ? null : String.valueOf(message.get("content"));

            if (content == null || content.isBlank() || "null".equals(content)) {
                throw new BizException(50301, "AI 服务未返回有效内容");
            }

            return content.trim();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(50301, "AI 服务暂时不可用，请稍后重试");
        }
    }

    /** 将 DeepSeek 返回的 SSE 文本增量交给调用方，不在这里保留完整回答。 */
    public void stream(String systemPrompt, String userPrompt, int maxTokens, Consumer<String> onDelta) {
        ensureConfigured();
        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "stream", true,
                "temperature", 0.2,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            deepSeekRestClient.post()
                    .uri(aiProperties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> headers.setBearerAuth(aiProperties.getApiKey()))
                    .body(requestBody)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new BizException(50301, "AI 服务暂时不可用，请稍后重试");
                        }
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                consumeStreamLine(line, onDelta);
                            }
                        }
                        return null;
                    });
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(50301, "AI 服务暂时不可用，请稍后重试");
        }
    }

    private void consumeStreamLine(String line, Consumer<String> onDelta) {
        if (!line.startsWith("data:")) {
            return;
        }
        String payload = line.substring("data:".length()).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            return;
        }
        try {
            Map<?, ?> event = objectMapper.readValue(payload, Map.class);
            List<?> choices = (List<?>) event.get("choices");
            if (choices == null || choices.isEmpty() || !(choices.get(0) instanceof Map<?, ?> first)) {
                return;
            }
            Map<?, ?> delta = (Map<?, ?>) first.get("delta");
            Object content = delta == null ? null : delta.get("content");
            if (content instanceof String text && StringUtils.hasText(text)) {
                onDelta.accept(text);
            }
        } catch (JacksonException exception) {
            throw new BizException(50301, "AI 服务返回了无法识别的内容");
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(aiProperties.getBaseUrl())
                || !StringUtils.hasText(aiProperties.getApiKey())
                || !StringUtils.hasText(aiProperties.getModel())) {
            throw new BizException(50301, "AI 服务尚未完成配置");
        }
    }
}
