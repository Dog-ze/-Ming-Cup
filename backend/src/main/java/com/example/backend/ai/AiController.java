package com.example.backend.ai;

import com.example.backend.ai.dto.BrewGuideRequest;
import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.ai.vo.BrewGuideVO;
import com.example.backend.ai.vo.AiChatStreamEvent;
import com.example.backend.common.Result;
import com.example.backend.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiBrewGuideService aiBrewGuideService;
    private final AiChatService aiChatService;
    @Qualifier("aiTaskExecutor")
    private final TaskExecutor aiTaskExecutor;

    @PostMapping("/brew-guides")
    public Result<BrewGuideVO> generateBrewGuide(@Valid @RequestBody BrewGuideRequest request) {
        return Result.ok(aiBrewGuideService.generate(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody AiChatRequest request) {
        Long userId = CurrentUser.id();
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> {
            try {
                aiChatService.stream(userId, request, content -> send(emitter, "delta", new AiChatStreamEvent(content)));
                send(emitter, "done", Map.of());
                emitter.complete();
            } catch (Exception exception) {
                try {
                    send(emitter, "error", Map.of("message", "AI 服务暂时不可用，请稍后重试"));
                } catch (Exception ignored) {
                    // 客户端主动断开时，SseEmitter 会拒绝继续写入；无需再次抛出。
                }
                emitter.complete();
            }
        }, aiTaskExecutor);
        return emitter;
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("流式响应已断开", exception);
        }
    }
}
