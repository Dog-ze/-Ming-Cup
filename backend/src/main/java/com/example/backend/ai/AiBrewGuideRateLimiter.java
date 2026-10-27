package com.example.backend.ai;

import com.example.backend.common.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/** 仅限制会触发第三方 AI 调用的请求，防止单个账号意外或恶意消耗额度。 */
@Component
public class AiBrewGuideRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final AiProperties properties;
    private final Cache<String, Deque<Instant>> requests = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    public AiBrewGuideRateLimiter(AiProperties properties) {
        this.properties = properties;
    }

    public void check(Long userId, String scene) {
        Deque<Instant> timestamps = requests.get(scene + ":" + userId, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized (timestamps) {
            Instant threshold = now.minus(WINDOW);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.removeFirst();
            }
            int limit = "chat".equals(scene) ? properties.getChatRequestsPerMinute()
                    : properties.getBrewGuideRequestsPerMinute();
            if (timestamps.size() >= limit) {
                String name = "chat".equals(scene) ? "AI 问茶" : "AI 泡法";
                throw new BizException(42901, name + "请求过于频繁，请稍后再试");
            }
            timestamps.addLast(now);
        }
    }
}
