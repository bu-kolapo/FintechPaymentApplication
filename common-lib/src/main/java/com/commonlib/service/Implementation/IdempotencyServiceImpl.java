package com.commonlib.service.Implementation;

import com.commonlib.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofMinutes(10); // key expiration

    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public <T> Mono<T> getResponse(String key, Class<T> clazz) {
        return redisTemplate.opsForValue()
                .get(key)
                .cast(clazz);
    }

    @Override
    public <T> Mono<Void> storeResponse(String key, T value) {
        return redisTemplate.opsForValue()
                .set(key, value, TTL)
                .then();
    }
}
