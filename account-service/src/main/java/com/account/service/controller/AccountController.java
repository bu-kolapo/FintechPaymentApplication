package com.account.service.controller;

import com.account.service.dto.AccountDTO;
import com.account.service.dto.AccountNotification;
import com.account.service.dto.AccountRequest;
import com.account.service.dto.AccountResponse;
import com.account.service.exception.AccountCreationException;
import com.account.service.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Accounts Creation", description = "Accounts management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {

    private final AccountService accountService;
    private final SimpMessagingTemplate messagingTemplate;

    public AccountController(AccountService accountService,
                             SimpMessagingTemplate messagingTemplate) {
        this.accountService = accountService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create/account")
    @Operation(
            summary = "Create an account using  customerId",
            description = "Create an account using  customerId. Requires an Idempotency-Key header to prevent duplicate creations.")
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            @RequestBody @Valid AccountRequest accountRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token){

        return accountService.createAccount(accountRequest, idempotencyKey,token)
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
    @Operation(summary = "Get account by ID")
    public Mono<ResponseEntity<AccountResponse>> getAccountById(@PathVariable String id) {
        return accountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(AccountCreationException.class, e ->
                        Mono.just(ResponseEntity.notFound().<AccountResponse>build())
                );
    }

    @GetMapping("/accounts")
    @Operation(summary = "Get all accounts", description = "Returns all accounts. Pass tenantId query param to filter by tenant.")
    public Flux<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PatchMapping("/account/{id}/balance")
    @Operation(summary = "Get  update accountBalance by ID")
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

    @GetMapping("/accounts/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public Mono<ResponseEntity<AccountResponse>> getAccountByAccountNumber(
            @PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
}