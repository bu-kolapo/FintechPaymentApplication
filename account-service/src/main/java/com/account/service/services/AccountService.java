package com.account.service.services;

import com.account.service.dto.AccountRequest;
import com.account.service.model.AccountResponse;
import reactor.core.publisher.Mono;

public interface AccountService {

    Mono<AccountResponse> createAccount(AccountRequest accountRequest);
}
