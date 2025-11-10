package com.account.service.repository;

import com.account.service.event.ProcessedEvent;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ProcessedEventRepository extends ReactiveMongoRepository<ProcessedEvent, String> {
    Mono<Boolean> existsByEventId(String eventId);
}