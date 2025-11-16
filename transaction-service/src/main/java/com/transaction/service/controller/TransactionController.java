package com.transaction.service.controller;

import com.transaction.service.model.Transaction;
import com.transaction.service.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ✅ Create a new transaction
    @PostMapping(value = ("/transactions"),produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transaction> createTransaction(@RequestBody Transaction transaction) {
        return transactionService.processTransaction(transaction);
    }

    // ✅ Get all transactions
    @GetMapping(value = ("/transactions"),produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    // ✅ Get a transaction by ID
    @GetMapping(value = "/transaction/account/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Transaction> getTransactionByAccountId(@PathVariable String id) {
        return transactionService.getTransactionsByAccountId(id);
    }

    @GetMapping(value = "/transaction/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Transaction> getTransactionById(@PathVariable String id) {
        return transactionService.getTransactionById(id);
    }
}
