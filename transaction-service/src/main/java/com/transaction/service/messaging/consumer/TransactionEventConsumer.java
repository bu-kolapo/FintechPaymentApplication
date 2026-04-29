package com.transaction.service.messaging.consumer;

import com.transaction.service.dto.TransactionResponse;
import com.transaction.service.messaging.Queues.QueueConstants;
import com.transaction.service.messaging.client.AccountClient;
import com.transaction.service.messaging.event.TransactionRequestEvent;
import com.transaction.service.messaging.publisher.TransactionEventPublisher;
import com.transaction.service.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final AccountClient accountClient;
    private final TransactionEventPublisher publisher;

    @RabbitListener(queues = QueueConstants.DEBIT_REQUEST_QUEUE)
    public void handleDebit(TransactionRequestEvent event) {
        process(event, Transaction.TransactionType.DEBIT);
    }

    @RabbitListener(queues = QueueConstants.CREDIT_REQUEST_QUEUE)
    public void handleCredit(TransactionRequestEvent event) {
        process(event, Transaction.TransactionType.CREDIT);
    }

    private void process(TransactionRequestEvent event,
                         Transaction.TransactionType type) {

        accountClient.getAccountById(event.getAccountId(), event.getToken())
                .flatMap(account -> {

                    BigDecimal newBalance;

                    if (type == Transaction.TransactionType.DEBIT) {
                        if (account.getBalance().compareTo(event.getAmount()) < 0) {
                            return publishFailure(event, type, "Insufficient balance");
                        }
                        newBalance = account.getBalance().subtract(event.getAmount());
                    } else {
                        newBalance = account.getBalance().add(event.getAmount());
                    }

                    return accountClient.updateAccountBalance(
                                    event.getAccountId(),
                                    newBalance,
                                    event.getToken()
                            )
                            .then(publishSuccess(event, type));
                })
                .onErrorResume(ex -> publishFailure(event, type, ex.getMessage()))
                .subscribe();
    }

    private Mono<Void> publishSuccess(TransactionRequestEvent event,
                                      Transaction.TransactionType type) {

        TransactionResponse response = TransactionResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountId(event.getAccountId())
                .idempotencyKey(event.getIdempotencyKey())
                .amount(event.getAmount())
                .type(type)
                .status(Transaction.TransactionStatus.SUCCESS)
                .tenantId(event.getTenantId())
                .token(event.getToken())
                .processedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        publisher.publishTransactionCompleted(response);

        log.info("✅ SUCCESS {} | key={}", type, event.getIdempotencyKey());

        return Mono.empty();
    }

    private Mono<Void> publishFailure(TransactionRequestEvent event,
                                      Transaction.TransactionType type,
                                      String reason) {

        TransactionResponse response = TransactionResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountId(event.getAccountId())
                .idempotencyKey(event.getIdempotencyKey())
                .amount(event.getAmount())
                .type(type)
                .status(Transaction.TransactionStatus.FAILED)
                .tenantId(event.getTenantId())
                .token(event.getToken())
                .message(reason)
                .processedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        publisher.publishTransactionCompleted(response);

        log.error("❌ FAILED {} | key={} | reason={}",
                type, event.getIdempotencyKey(), reason);

        return Mono.empty();
    }
}