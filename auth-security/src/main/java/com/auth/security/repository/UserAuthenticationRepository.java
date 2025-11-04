package com.auth.security.repository;

import com.auth.security.model.UserAuthentication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;


public interface UserAuthenticationRepository extends ReactiveMongoRepository<UserAuthentication, String> {
    Mono<UserAuthentication> findByUsername(String username);

}