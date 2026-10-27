package com.example.backend.auth;

import com.example.backend.common.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private final Cache<String, AtomicInteger> failures = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)).maximumSize(100_000).build();

    public void check(String key) {
        AtomicInteger count = failures.getIfPresent(key);
        if (count != null && count.get() >= MAX_FAILURES) {
            throw new BizException(42902, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    public void failure(String key) {
        failures.get(key, ignored -> new AtomicInteger()).incrementAndGet();
    }

    public void success(String key) {
        failures.invalidate(key);
    }
}
