package com.apiplatform.security.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guards {@code /api/auth/signin} against credential-stuffing / brute-force attempts.
 * <p>
 * There was previously no rate limiting anywhere in the application, so an attacker
 * could attempt unlimited password guesses per second against any known email address.
 * <p>
 * This is an in-memory, single-instance limiter — good enough for a small deployment
 * or a single container, but it will NOT coordinate across multiple replicas behind a
 * load balancer. For horizontal scaling, replace this with a shared store (Redis +
 * Bucket4j, or an API gateway / WAF rate-limiting rule) — noted as a follow-up in
 * AUDIT.md.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private record Window(AtomicInteger count, long windowStart) {
    }

    private final ConcurrentHashMap<String, Window> attemptsByKey = new ConcurrentHashMap<>();

    /**
     * @param key typically the client IP, optionally combined with the submitted email
     * @return true if the caller is still within the allowed rate
     */
    public boolean tryAcquire(String key) {
        long now = Instant.now().toEpochMilli();
        Window window = attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() > WINDOW_MILLIS) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= MAX_ATTEMPTS;
    }
}
