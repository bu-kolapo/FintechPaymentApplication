package com.account.service.event.listener;

import com.account.service.event.TransactionCompletedEvent;
import com.account.service.repository.AccountRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionEventListener {

    private final AccountRepository accountRepository;

    public TransactionEventListener(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @RabbitListener(queues = "transaction-completed-queue")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        accountRepository.findById(event.getAccountId())
                .flatMap(account -> {
                    BigDecimal newBalance = event.getType().equalsIgnoreCase("CREDIT")
                            ? account.getBalance().add(event.getAmount())
                            : account.getBalance().subtract(event.getAmount());

                    account.setBalance(newBalance);
                    account.setUpdatedAt(java.time.Instant.now());
                    return accountRepository.save(account);
                })
                .doOnSuccess(acc -> System.out.println("✅ Account updated for transaction: " + event.getTransactionId()))
                .doOnError(err -> System.err.println("❌ Error updating account: " + err.getMessage()))
                .subscribe();
    }
}