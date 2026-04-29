package com.transaction.service.services.Implementation;

import com.commonlib.service.IdempotencyService;
import com.transaction.service.messaging.client.AccountClient;
import com.transaction.service.dto.TransactionResponse;
import com.transaction.service.model.Transaction;
import com.transaction.service.messaging.publisher.TransactionEventPublisher;
import com.transaction.service.repository.TransactionRepository;
import com.transaction.service.services.TransactionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AccountClient accountClient;
    private final TransactionEventPublisher eventPublisher;

    private final IdempotencyService idempotencyService;

    @Override
    @CircuitBreaker(name = "transactionServiceCB", fallbackMethod = "transactionFallback")
    @RateLimiter(name = "transactionServiceRL")
    public Mono<TransactionResponse> processTransaction(Transaction transaction, String idempotencyKey, String token) {
        log.info("🔑 Token received in processTransaction: {}", token); // ← add this
        log.info("📦 AccountId: {}", transaction.getAccountId());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new RuntimeException("Idempotency-Key header is required"));
        }

        return idempotencyService.exists(idempotencyKey)
                .flatMap(exists -> {

                    // ✅ Idempotency: return stored response
                    if (exists) {
                        return idempotencyService.getResponse(idempotencyKey, TransactionResponse.class);
                    }

                    // ✅ Continue processing
                    return accountClient.getAccountById(transaction.getAccountId(), token)
                            .flatMap(accountDTO -> {

                                BigDecimal currentBalance = accountDTO.getBalance();
                                BigDecimal txnAmount = transaction.getAmount();

                            // Handle DEBIT logic
                            if (transaction.getType() == Transaction.TransactionType.DEBIT
                                    && currentBalance.compareTo(txnAmount) < 0) {
                                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                                transaction.setDescription("Insufficient funds");
                                transaction.setProcessedAt(Instant.now());

                                TransactionResponse failedResponse = TransactionResponse.builder()
                                        .id(transaction.getId())
                                        .tenantId(transaction.getTenantId())
                                        .accountId(transaction.getAccountId())
                                        .idempotencyKey(transaction.getIdempotencyKey())
                                        .type(transaction.getType())
                                        .amount(transaction.getAmount())
                                        .status(transaction.getStatus())
                                        .description(transaction.getDescription())
                                        .processedAt(transaction.getProcessedAt())
                                        .createdAt(transaction.getCreatedAt())
                                        .message("Transaction failed: Insufficient funds")
                                        .build();

                                return idempotencyService.storeResponse(idempotencyKey, failedResponse)
                                        .thenReturn(failedResponse);
                            }

                            // Compute new balance
                            BigDecimal newBalance = transaction.getType() == Transaction.TransactionType.DEBIT
                                    ? currentBalance.subtract(txnAmount)
                                    : currentBalance.add(txnAmount);

                            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
                            transaction.setIdempotencyKey(idempotencyKey);
                            transaction.setProcessedAt(Instant.now());

                            // Update account balance via WebClient
                            return accountClient.updateAccountBalance(transaction.getAccountId(), newBalance,token)
                                    .then(transactionRepository.save(transaction))
                                    .map(savedTxn -> TransactionResponse.builder()
                                            .id(savedTxn.getId())
                                            .tenantId(savedTxn.getTenantId())
                                            .accountId(savedTxn.getAccountId())
                                            .idempotencyKey(savedTxn.getIdempotencyKey())
                                            .type(savedTxn.getType())
                                            .amount(savedTxn.getAmount())
                                            .status(savedTxn.getStatus())
                                            .description(savedTxn.getDescription())
                                            .processedAt(savedTxn.getProcessedAt())
                                            .createdAt(savedTxn.getCreatedAt())
                                            .message("Transaction processed successfully")
                                            .build())
                                    .flatMap(response -> idempotencyService.storeResponse(idempotencyKey, response)
                                            .thenReturn(response))
                                    .doOnSuccess(eventPublisher::publishTransactionCompleted);
                        });
            });
}






    public Mono<Transaction> transactionFallback(Transaction request, String idempotencyKey, String token, Throwable throwable) {
        Transaction fallbackTransaction = new Transaction();
        fallbackTransaction.setId(request.getId());
        fallbackTransaction.setAccountId(request.getAccountId());
        fallbackTransaction.setTenantId(request.getTenantId());
        fallbackTransaction.setAmount(request.getAmount());
        fallbackTransaction.setType(request.getType());
        fallbackTransaction.setStatus(Transaction.TransactionStatus.FAILED);
        fallbackTransaction.setDescription("Fallback triggered: " + throwable.getMessage());
        fallbackTransaction.setProcessedAt(Instant.now());
        return Mono.just(fallbackTransaction);
    }





    public Flux<Transaction> getAllTransactions() {
        return transactionRepository.findAll()
                .doOnSubscribe(sub -> System.out.println("📘 Fetching all transactions..."))
                .doOnComplete(() -> System.out.println("✅ Completed fetching all transactions"));
    }

    public Flux<Transaction> getTransactionsByAccountId(String accountId) {
        return transactionRepository.findByAccountId(accountId)
                .switchIfEmpty(Mono.error(new RuntimeException("❌ No transactions found for accountId: " + accountId)))
                .doOnComplete(() -> System.out.println("📘 Fetched transactions for accountId: " + accountId));
    }

    public Mono<Transaction> getTransactionById(String id)
    { return transactionRepository.findById(id) .switchIfEmpty(Mono.error(new RuntimeException("❌ Transaction not found with id: " + id)))
            .doOnSuccess(tx -> System.out.println("📘 Fetched transaction with id: " + id));
    }
}


