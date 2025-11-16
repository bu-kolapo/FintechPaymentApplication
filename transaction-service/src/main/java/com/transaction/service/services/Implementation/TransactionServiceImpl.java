package com.transaction.service.services.Implementation;

import com.transaction.service.client.AccountClient;
import com.transaction.service.config.RabbitMQConfig;
import com.transaction.service.dto.AccountDTO;
import com.transaction.service.event.TransactionCompletedEvent;
import com.transaction.service.model.Transaction;
import com.transaction.service.publisher.TransactionEventPublisher;
import com.transaction.service.repository.TransactionRepository;
import com.transaction.service.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AccountClient accountClient;
    private final TransactionEventPublisher eventPublisher;

//    public TransactionServiceImpl(TransactionRepository transactionRepository, RabbitTemplate rabbitTemplate,AccountClient accountClient,TransactionEventPublisher eventPublisher) {
//        this.transactionRepository = transactionRepository;
//        this.rabbitTemplate = rabbitTemplate;
//        this.accountClient=accountClient;
//        this.eventPublisher=eventPublisher;
//    }
    @Override
    public Mono<Transaction> processTransaction(Transaction transaction) {
        log.info("🔵 INCOMING transaction - accountId: '{}', idempotencyKey: '{}'",
                transaction.getAccountId(), transaction.getIdempotencyKey());
        return transactionRepository.findByIdempotencyKey(transaction.getIdempotencyKey())
                .switchIfEmpty(
                        accountClient.getAccountById(transaction.getAccountId())
                                .flatMap(account -> {
                                    BigDecimal currentBalance = account.getBalance();
                                    BigDecimal txnAmount = transaction.getAmount();

                                    // Handle DEBIT / CREDIT
                                    if (transaction.getType() == Transaction.TransactionType.DEBIT) {
                                        if (currentBalance.compareTo(txnAmount) < 0) {
                                            transaction.setStatus(Transaction.TransactionStatus.FAILED);
                                            transaction.setDescription("Insufficient funds");
                                            transaction.setProcessedAt(Instant.now());
                                            return Mono.just(transaction);
                                        }
                                        account.setBalance(currentBalance.subtract(txnAmount));
                                    } else if (transaction.getType() == Transaction.TransactionType.CREDIT) {
                                        account.setBalance(currentBalance.add(txnAmount));
                                    }

                                    transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
                                    transaction.setProcessedAt(Instant.now());

                                    // ✅ Call the client method here
                                    return accountClient.updateAccountBalance(transaction.getAccountId(), account.getBalance())
                                            .then(transactionRepository.save(transaction))
                                            .doOnSuccess(eventPublisher::publishTransactionCompleted);
                                })
                );
    }



    public Flux<Transaction> getAllTransactions() {
        return transactionRepository.findAll()
                .doOnSubscribe(sub -> System.out.println("📘 Fetching all transactions..."))
                .doOnComplete(() -> System.out.println("✅ Completed fetching all transactions"));
    }

    /**
     * ✅ Retrieve a single transaction by ID
     */
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


