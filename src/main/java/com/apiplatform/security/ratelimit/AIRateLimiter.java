package com.apiplatform.security.ratelimit;

import com.apiplatform.config.AIProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guards {@code /api/v1/ai/**} against a single user firing an unbounded number of LLM
 * calls -- each of which is genuinely expensive (latency, and real quota/cost against
 * whichever provider is configured), unlike a normal CRUD endpoint. Same in-memory,
 * single-instance design and same caveat as {@link LoginRateLimiter}: fine for one
 * container, needs a shared store (Redis) once this runs behind multiple replicas.
 */
@Component
public class AIRateLimiter {

    private record Window(AtomicInteger count, long windowStart) {
    }

    private final ConcurrentHashMap<Long, Window> attemptsByUserId = new ConcurrentHashMap<>();
    private final AIProperties properties;

    public AIRateLimiter(AIProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquire(Long userId) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = properties.getRateLimit().getWindowSeconds() * 1000L;
        int maxRequests = properties.getRateLimit().getMaxRequests();

        Window window = attemptsByUserId.compute(userId, (k, existing) -> {
            if (existing == null || now - existing.windowStart() > windowMillis) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= maxRequests;
    }
}
