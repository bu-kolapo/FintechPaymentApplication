package com.transaction.service.messaging.client;

import com.transaction.service.dto.AccountDTO;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountClient {


        Mono<AccountDTO> getAccountById(String accountId,String token);
        Mono<AccountDTO> updateAccountBalance(String accountId, BigDecimal newBalance, String token);

}
