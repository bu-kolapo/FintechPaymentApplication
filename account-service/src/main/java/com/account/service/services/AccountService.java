package com.account.service.services;

import com.account.service.dto.AccountDTO;
import com.account.service.dto.AccountRequest;
import com.account.service.dto.AccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountService {

    Mono<AccountResponse> createAccount(AccountRequest accountRequest,String idempotencyKey,String token);
    Mono<AccountResponse>  getAccountById(String id);
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountDTO> updateBalance(String accountId, BigDecimal newBalance);
    Flux<AccountResponse> getAccountsByTenant(String tenantId);
   Mono<AccountResponse> getAccountByNumber(String accountNumber);
    }

