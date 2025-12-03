package com.transaction.service.client;

import com.commonlib.util.JwtUtil;
import com.transaction.service.dto.AccountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

    @Component
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


        @Override
        public Mono<AccountDTO> getAccountById(String accountId) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> jwtUtil.extractToken(ctx.getAuthentication()))
                    .defaultIfEmpty(null)
                    .flatMap(token ->
                            webClient.get()
                                    .uri("/api/v1/account/{id}", accountId)
                                    .headers(headers -> {
                                        if (token != null && !token.isBlank()) {
                                            headers.setBearerAuth(token); // <-- JWT forwarded
                                        }
                                    })
                                    .retrieve()
                                    .bodyToMono(AccountDTO.class)
                    );
        }


        @Override
        public Mono<AccountDTO> updateAccountBalance(String accountId, BigDecimal newBalance) {
            // Validate inputs
            if (accountId == null || accountId.isBlank()) {
                return Mono.error(new IllegalArgumentException("Account ID cannot be null or empty"));
            }
            if (newBalance == null) {
                return Mono.error(new IllegalArgumentException("New balance cannot be null"));
            }
            return webClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/account/{id}/balance")
                            .build(accountId)) // ✅ safely injects the accountId
                    .bodyValue(Map.of("balance", newBalance))
                    .retrieve()
                    .bodyToMono(AccountDTO.class);
        }
        }


        // 👇 PUT extractToken() here (inside the class, but outside other method]


