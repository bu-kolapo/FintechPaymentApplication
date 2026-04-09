package com.account.service.controller;

import com.account.service.dto.AccountDTO;
import com.account.service.dto.AccountNotification;
import com.account.service.dto.AccountRequest;
import com.account.service.dto.AccountResponse;
import com.account.service.exception.AccountCreationException;
import com.account.service.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;
    private final SimpMessagingTemplate messagingTemplate;

    public AccountController(AccountService accountService,
                             SimpMessagingTemplate messagingTemplate) {
        this.accountService = accountService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create/account")
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            @RequestBody @Valid AccountRequest accountRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return accountService.createAccount(accountRequest, idempotencyKey)
                .doOnSuccess(response ->
                        messagingTemplate.convertAndSend(
                                "/topic/accounts",
                                new AccountNotification(
                                        response.getId(),
                                        response.getAccountNumber(),
                                        "ACCOUNT_CREATED",
                                        Instant.now()
                                )
                        )
                )
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity
                                .badRequest()
                                .<AccountResponse>build())
                )
                .onErrorResume(AccountCreationException.class, e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .<AccountResponse>build())
                );
    }

    @GetMapping("/account/{id}")
    public Mono<ResponseEntity<AccountResponse>> getAccountById(@PathVariable String id) {
        return accountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(AccountCreationException.class, e ->
                        Mono.just(ResponseEntity.notFound().<AccountResponse>build())
                );
    }

    @GetMapping("/accounts")
    public Flux<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PatchMapping("/account/{id}/balance")
    public Mono<ResponseEntity<AccountDTO>> updateBalance(
            @PathVariable String id,
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal newBalance = body.get("balance");

        if (newBalance == null) {
            return Mono.just(ResponseEntity.badRequest().<AccountDTO>build());
        }

        return accountService.updateBalance(id, newBalance)
                .map(ResponseEntity::ok)
                .onErrorResume(AccountCreationException.class, e ->
                        Mono.just(ResponseEntity.notFound().<AccountDTO>build())
                );
    }
}