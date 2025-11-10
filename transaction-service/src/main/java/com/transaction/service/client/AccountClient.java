package com.transaction.service.client;

import com.transaction.service.dto.AccountDTO;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountClient {


        Mono<AccountDTO> getAccountById(String accountId);
        Mono<AccountDTO> updateAccountBalance(String accountId, BigDecimal newBalance);

}
