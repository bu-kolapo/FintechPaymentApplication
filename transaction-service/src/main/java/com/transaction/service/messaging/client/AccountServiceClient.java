package com.transaction.service.messaging.client;

import com.commonlib.util.JwtUtil;
import com.transaction.service.dto.AccountDTO;
import com.transaction.service.messaging.client.AccountClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

    @Component
    @Slf4j
    public class AccountServiceClient implements AccountClient {
        @Autowired
        private JwtUtil jwtUtil;


//        @Value("${account.service.base-url}")
//        private String accountServiceBaseUrl;


        private final WebClient webClient;

//        public AccountServiceClient(WebClient.Builder builder) {
//            this.webClient = builder.baseUrl(accountServiceBaseUrl).build();
//        }


        public AccountServiceClient(WebClient.Builder builder,
                                    @Value("${account.service.base-url}") String baseUrl) {
            // Only the host:port goes here, no path or {id}
            this.webClient = builder.baseUrl(baseUrl).build();
        }


        public Mono<AccountDTO> getAccountById(String accountId, String token) {
            log.info("🌐 Calling account service for id: {} with token: {}", accountId, token);
            return webClient.get()
                    .uri("/api/v1/account/{id}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, token) // ✅ forward JWT exactly like your other method
                    .retrieve()
                    .onStatus(status -> status.value() == 401, response ->
                            Mono.error(new RuntimeException(
                                    "Unauthorized to access account service")))
                    .onStatus(status -> status.value() == 404, response ->
                            Mono.error(new RuntimeException(
                                    "Account not found for id: " + accountId)))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            Mono.error(new RuntimeException(
                                    "Account service error for id: " + accountId)))
                    .bodyToMono(AccountDTO.class)
                    .doOnSuccess(account ->
                            log.info("✅ Resolved accountId {} → balance {}",
                                    accountId, account.getBalance()))
                    .doOnError(err ->
                            log.error("❌ Failed to fetch account for id {}: {}",
                                    accountId, err.getMessage()));
        }

        @Override
        public Mono<AccountDTO> updateAccountBalance(String accountId, BigDecimal newBalance, String token) {
            if (accountId == null || accountId.isBlank()) {
                return Mono.error(new IllegalArgumentException("Account ID cannot be null or empty"));
            }
            if (newBalance == null) {
                return Mono.error(new IllegalArgumentException("New balance cannot be null"));
            }

            return webClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/account/{id}/balance")
                            .build(accountId))
                    .header(HttpHeaders.AUTHORIZATION, token) // ✅ forward JWT
                    .bodyValue(Map.of("balance", newBalance))
                    .retrieve()
                    .onStatus(status -> status.value() == 401, response ->
                            Mono.error(new RuntimeException(
                                    "Unauthorized to access account service")))
                    .onStatus(status -> status.value() == 404, response ->
                            Mono.error(new RuntimeException(
                                    "Account not found for id: " + accountId)))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            Mono.error(new RuntimeException(
                                    "Account service error for id: " + accountId)))
                    .bodyToMono(AccountDTO.class)
                    .doOnSuccess(account ->
                            log.info("✅ Balance updated for accountId {} → new balance {}",
                                    accountId, account.getBalance()))
                    .doOnError(err ->
                            log.error("❌ Failed to update balance for accountId {}: {}",
                                    accountId, err.getMessage()));
        }

        // 👇 PUT extractToken() here (inside the class, but outside other method]

    }
