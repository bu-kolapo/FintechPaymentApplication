package com.commonlib.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GlobalRateLimiter {

    private final RateLimiter rateLimiter;

    public GlobalRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)                 // Max 10 requests per period
                .limitRefreshPeriod(Duration.ofSeconds(1))  // Reset every 1 second
                .timeoutDuration(Duration.ZERO)     // Immediately reject excess
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter("globalRateLimiter"); // single shared instance
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}

