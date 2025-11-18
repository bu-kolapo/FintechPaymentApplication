package com.commonlib.service;

import com.commonlib.util.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;


public interface IdempotencyService {

    Mono<Boolean> exists(String key);

    <T> Mono<T> getResponse(String key, Class<T> clazz);

    <T> Mono<Void> storeResponse(String key, T value);
}