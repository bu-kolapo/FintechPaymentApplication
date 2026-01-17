package com.transaction.service.services;

import com.transaction.service.dto.TransactionResponse;
import com.transaction.service.model.Transaction;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Mono<TransactionResponse> processTransaction(Transaction transaction);
    Flux<Transaction> getAllTransactions();
    Flux<Transaction> getTransactionsByAccountId(String accountId);
    Mono<Transaction> getTransactionById(String id);

}
