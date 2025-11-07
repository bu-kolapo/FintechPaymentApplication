package com.account.service.services;

import com.account.service.dto.AccountRequest;
import com.account.service.dto.AccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountService {

    Mono<AccountResponse> createAccount(AccountRequest accountRequest);
    Flux<AccountResponse> getAllAccounts();
    Flux<AccountResponse> getAccountsByTenant(String tenantId);
}
