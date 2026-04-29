package com.commonlib.service.Implementation;

import com.commonlib.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyServiceImpl(ReactiveStringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public <T> Mono<Void> storeResponse(String key, T response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            return redisTemplate.opsForValue()
                    .set(key, json, Duration.ofMinutes(10)) // ✅ TTL added here
                    .then();
        } catch (Exception e) {
            return Mono.error(new RuntimeException("JSON serialization failed", e));
        }
    }

    @Override
    public <T> Mono<T> getResponse(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("No cached response for key: " + key)))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, clazz));
                    } catch (Exception e) {
                        return Mono.error(
                                new RuntimeException("JSON deserialization failed", e));
                    }
                });
    }
}

