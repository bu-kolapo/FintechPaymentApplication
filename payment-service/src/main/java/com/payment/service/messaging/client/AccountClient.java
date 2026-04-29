package com.payment.service.messaging.client;

import com.payment.service.dto.AccountDTO;
import com.payment.service.exception.AccountServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.security.auth.login.AccountNotFoundException;

@Component
@Slf4j
public class AccountClient {

    private final WebClient webClient;

    public AccountClient(@Value("${account.service.url}") String accountServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    public Mono<AccountDTO> getAccountByNumber(String accountNumber, String token) {
        return webClient.get()
                .uri("/api/v1/accounts/number/{accountNumber}", accountNumber)
                .header(HttpHeaders.AUTHORIZATION, token) // ✅ forward JWT
                .retrieve()
                .onStatus(status -> status.value() == 401, response ->
                        Mono.error(new RuntimeException(
                                "Unauthorized to access account service")))
                .onStatus(status -> status.value() == 404, response ->
                        Mono.error(new AccountNotFoundException(
                                "Account not found for number: " + accountNumber)))
                .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new AccountServiceException(
                                "Account service error for number: " + accountNumber)))
                .bodyToMono(AccountDTO.class)
                .doOnSuccess(account ->
                        log.info("✅ Resolved account number {} → accountId {}",
                                accountNumber, account.getId()))
                .doOnError(err ->
                        log.error("❌ Failed to fetch account for number {}: {}",
                                accountNumber, err.getMessage()));
    }
}