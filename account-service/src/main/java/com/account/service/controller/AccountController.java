package com.account.service.controller;

import com.account.service.dto.AccountNotification;
import com.account.service.dto.AccountRequest;
import com.account.service.exception.AccountCreationException;
import com.account.service.model.Account;
import com.account.service.model.AccountResponse;
import com.account.service.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private  final AccountService accountService;
    private final SimpMessagingTemplate messagingTemplate;

    public AccountController(AccountService accountService, SimpMessagingTemplate messagingTemplate) {
        this.accountService = accountService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create/account")
    public Mono<ResponseEntity<AccountResponse>> createCustomer(
            @RequestBody @Valid AccountRequest accountRequest) {

        return accountService.createAccount(accountRequest)
                .doOnSuccess(response -> {
                    // Send WebSocket notification to all connected clients
                    messagingTemplate.convertAndSend(
                            "/topic/accounts",
                            new AccountNotification(
                                    response.getId(),
                                    response.getAccountNumber(),
                                    "ACCOUNT_CREATED",
                                    Instant.now()
                            )
                    );
                })
                .map(ResponseEntity::ok)
                .onErrorResume(AccountCreationException.class, e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new AccountResponse(null, e.getMessage()))
                        )
                );
    }

}
