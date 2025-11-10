package com.transaction.service.services;

import com.transaction.service.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

    Mono<Transaction> processTransaction(Transaction transaction);
    Flux<Transaction> getAllTransactions();
    Mono<Transaction> getTransactionById(String id);
}
